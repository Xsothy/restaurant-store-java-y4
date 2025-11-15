package com.restaurant.store.service;

import com.restaurant.store.controller.api.OrderStatusWebSocketController;
import com.restaurant.store.dto.request.CreateOrderRequest;
import com.restaurant.store.dto.request.OrderItemRequest;
import com.restaurant.store.dto.request.PaymentRequest;
import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.entity.*;
import com.restaurant.store.exception.BadRequestException;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.integration.AdminIntegrationService;
import com.restaurant.store.mapper.OrderMapper;
import com.restaurant.store.repository.*;
import com.restaurant.store.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryRepository deliveryRepository;
    private final JwtUtil jwtUtil;
    private final OrderMapper orderMapper;
    private final PaymentIntentService paymentIntentService;
    private final AdminIntegrationService adminIntegrationService;
    private final CartService cartService;
    private final OrderStatusWebSocketController orderStatusWebSocketController;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String token) {
        // Extract customer from token
        String email = jwtUtil.extractUsername(token.substring(7));
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Validate and calculate total price
        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemRequest : request.getOrderItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));

            if (!product.getIsAvailable()) {
                throw new BadRequestException("Product " + product.getName() + " is not available");
            }

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalPrice = totalPrice.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setSpecialInstructions(itemRequest.getSpecialInstructions());
            orderItems.add(orderItem);
        }

        // Create order
        Order order = new Order();
        order.setCustomer(customer);
        order.setTotalPrice(totalPrice);
        order.setOrderType(request.getOrderType());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setPhoneNumber(request.getPhoneNumber());
        order.setSpecialInstructions(request.getSpecialInstructions());
        order.setStatus(OrderStatus.PENDING);
        order.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(30));

        order = orderRepository.save(order);

        // Link order items to order
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        orderItemRepository.saveAll(orderItems);

        // Create delivery record if order type is DELIVERY
        if (request.getOrderType() == OrderType.DELIVERY) {
            Delivery delivery = new Delivery();
            delivery.setOrder(order);
            delivery.setDeliveryAddress(request.getDeliveryAddress());
            delivery.setPhoneNumber(request.getPhoneNumber());
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setEstimatedDeliveryTime(order.getEstimatedDeliveryTime());
            deliveryRepository.save(delivery);
        }

        // Sync order to Admin backend
        try {
            log.info("Syncing order {} to Admin backend", order.getId());
            adminIntegrationService.syncOrderToAdmin(order.getId());
        } catch (Exception e) {
            log.error("Failed to sync order to Admin backend", e);
        }

        List<OrderItem> persistedItems = orderItemRepository.findByOrderId(order.getId());
        return orderMapper.toResponse(order, persistedItems);
    }

    public OrderResponse getOrderById(Long orderId, String token) {
        Customer customer = getCustomerFromToken(token);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Verify order belongs to customer
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Order does not belong to current customer");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return enrichOrderResponse(orderMapper.toResponse(order, orderItems), order);
    }

    public String getOrderStatus(Long orderId, String token) {
        Customer customer = getCustomerFromToken(token);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Order does not belong to current customer");
        }

        return order.getStatus().toString();
    }

    @Transactional
    public Map<String, Object> createPaymentIntent(Long orderId, String token) {
        Customer customer = getCustomerFromToken(token);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Order does not belong to current customer");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot process payment for cancelled order");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Order already delivered and paid");
        }

        try {
            return paymentIntentService.createPayment(order)
                    .toMap();
        } catch (Exception e) {
            log.error("Error creating payment intent for order {}", orderId, e);
            throw new BadRequestException("Failed to create payment intent: " + e.getMessage());
        }
    }

    @Transactional
    public String processPayment(Long orderId, PaymentRequest request, String token) {
        Customer customer = getCustomerFromToken(token);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Order does not belong to current customer");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot process payment for cancelled order");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Order already delivered and paid");
        }

        // For non-Stripe payments (e.g., Cash on Delivery)
        if (request.getPaymentMethod() != PaymentMethod.STRIPE &&
            request.getPaymentMethod() != PaymentMethod.CREDIT_CARD &&
            request.getPaymentMethod() != PaymentMethod.DEBIT_CARD) {
            Payment payment = paymentRepository
                    .findFirstByOrderIdAndMethodAndStatusOrderByUpdatedAtDesc(
                            order.getId(),
                            request.getPaymentMethod(),
                            PaymentStatus.CASH_PENDING
                    )
                    .orElseGet(Payment::new);

            payment.setOrder(order);
            payment.setAmount(order.getTotalPrice());
            payment.setMethod(request.getPaymentMethod());
            payment.setStatus(PaymentStatus.CASH_PENDING);
            if (payment.getTransactionId() == null) {
                payment.setTransactionId("COD-" + UUID.randomUUID());
            }
            payment.setPaidAt(null);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            return "Payment processed successfully";
        }

        // For Stripe payments, confirm the payment intent
        try {
            paymentIntentService.handlePaymentSuccess(request.getTransactionId());
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            return "Payment processed successfully";
        } catch (Exception e) {
            log.error("Error processing payment for order {}", orderId, e);
            throw new BadRequestException("Failed to process payment: " + e.getMessage());
        }
    }

    public List<OrderResponse> getCustomerOrders(Long customerId, String token) {
        if (token == null) {
            return getOrdersForCustomer(customerId);
        }

        Customer customer = getCustomerFromToken(token);

        // Verify requesting customer matches the customerId
        if (!customer.getId().equals(customerId)) {
            throw new BadRequestException("Cannot access other customer's orders");
        }

        return getOrdersForCustomer(customerId);
    }

    public List<OrderResponse> getMyOrders(String token) {
        Customer customer = getCustomerFromToken(token);
        return getOrdersForCustomer(customer.getId());
    }

    @Transactional
    public String cancelOrder(Long orderId, String token) {
        Customer customer = getCustomerFromToken(token);
        cancelOrderForCustomer(orderId, customer.getId());
        return "Order cancelled successfully";
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        List<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        return orders.stream()
                .map(order -> enrichOrderResponse(
                        orderMapper.toResponse(order, orderItemRepository.findByOrderId(order.getId())),
                        order))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderForCustomer(Long orderId, Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Order does not belong to current customer");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return enrichOrderResponse(orderMapper.toResponse(order, orderItems), order);
    }

    @Transactional
    public OrderResponse cancelOrderForCustomer(Long orderId, Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Order does not belong to current customer");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Cannot cancel delivered order");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        OrderResponse response = enrichOrderResponse(orderMapper.toResponse(order, orderItems), order);
        orderStatusWebSocketController.sendOrderUpdate(orderId, response);
        orderStatusWebSocketController.sendOrderStatusUpdate(orderId, order.getStatus().name());
        return response;
    }

    private OrderResponse enrichOrderResponse(OrderResponse response, Order order) {
        if (response == null || order == null) {
            return response;
        }

        List<Payment> payments = paymentRepository.findByOrderId(order.getId());
        if (!payments.isEmpty()) {
            payments.sort(Comparator.comparing(Payment::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            Payment latestPayment = payments.get(0);
            response.setPaymentStatus(latestPayment.getStatus());
            response.setPaymentMethod(latestPayment.getMethod());
            response.setPaymentPaidAt(latestPayment.getPaidAt());
            response.setPaymentTransactionId(latestPayment.getTransactionId());
        }

        Optional<Delivery> deliveryOptional = deliveryRepository.findByOrderId(order.getId());
        if (deliveryOptional.isPresent()) {
            Delivery delivery = deliveryOptional.get();
            response.setDeliveryStatus(delivery.getStatus());
            response.setDeliveryDriverName(delivery.getDriverName());
            response.setDeliveryDriverPhone(delivery.getDriverPhone());
            response.setDeliveryEstimatedArrivalTime(delivery.getEstimatedArrivalTime());
            response.setDeliveryActualDeliveryTime(delivery.getActualDeliveryTime());
        }

        return response;
    }

    private Customer getCustomerFromToken(String token) {
        String email = jwtUtil.extractUsername(token.substring(7));
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }
}

