package com.restaurant.store.service;

import com.restaurant.store.dto.request.CreateOrderRequest;
import com.restaurant.store.dto.request.OrderItemRequest;
import com.restaurant.store.dto.request.PaymentRequest;
import com.restaurant.store.dto.response.OrderItemResponse;
import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.entity.*;
import com.restaurant.store.exception.BadRequestException;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.repository.*;
import com.restaurant.store.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private JwtUtil jwtUtil;

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

        return mapToOrderResponse(order, orderItems);
    }

    public OrderResponse getOrderById(Long orderId, String token) {
        String email = jwtUtil.extractUsername(token.substring(7));
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Verify order belongs to customer
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Order does not belong to current customer");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return mapToOrderResponse(order, orderItems);
    }

    public String getOrderStatus(Long orderId, String token) {
        String email = jwtUtil.extractUsername(token.substring(7));
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Order does not belong to current customer");
        }

        return order.getStatus().toString();
    }

    @Transactional
    public String processPayment(Long orderId, PaymentRequest request, String token) {
        String email = jwtUtil.extractUsername(token.substring(7));
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

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

        // Create payment record
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalPrice());
        payment.setMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Update order status
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        return "Payment processed successfully";
    }

    public List<OrderResponse> getCustomerOrders(Long customerId, String token) {
        String email = jwtUtil.extractUsername(token.substring(7));
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Verify requesting customer matches the customerId
        if (!customer.getId().equals(customerId)) {
            throw new BadRequestException("Cannot access other customer's orders");
        }

        List<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return orders.stream()
                .map(order -> mapToOrderResponse(order, orderItemRepository.findByOrderId(order.getId())))
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getMyOrders(String token) {
        String email = jwtUtil.extractUsername(token.substring(7));
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        List<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        return orders.stream()
                .map(order -> mapToOrderResponse(order, orderItemRepository.findByOrderId(order.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public String cancelOrder(Long orderId, String token) {
        String email = jwtUtil.extractUsername(token.substring(7));
        Customer customer = customerRepository.findByEmail(email)
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
        orderRepository.save(order);

        return "Order cancelled successfully";
    }

    private OrderResponse mapToOrderResponse(Order order, List<OrderItem> orderItems) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomer().getId());
        response.setCustomerName(order.getCustomer().getName());
        response.setStatus(order.getStatus());
        response.setTotalPrice(order.getTotalPrice());
        response.setOrderType(order.getOrderType());
        response.setDeliveryAddress(order.getDeliveryAddress());
        response.setPhoneNumber(order.getPhoneNumber());
        response.setSpecialInstructions(order.getSpecialInstructions());
        response.setCreatedAt(order.getCreatedAt());
        response.setEstimatedDeliveryTime(order.getEstimatedDeliveryTime());

        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());
        response.setOrderItems(itemResponses);

        return response;
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem orderItem) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(orderItem.getId());
        response.setProductId(orderItem.getProduct().getId());
        response.setProductName(orderItem.getProduct().getName());
        response.setQuantity(orderItem.getQuantity());
        response.setTotalPrice(orderItem.getPrice());
        response.setSpecialInstructions(orderItem.getSpecialInstructions());
        return response;
    }
}
