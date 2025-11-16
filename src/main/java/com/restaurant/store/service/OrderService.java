package com.restaurant.store.service;

import com.restaurant.store.controller.api.OrderStatusWebSocketController;
import com.restaurant.store.dto.request.CreateOrderRequest;
import com.restaurant.store.dto.request.OrderItemRequest;
import com.restaurant.store.dto.request.PaymentRequest;
import com.restaurant.store.dto.response.CartItemResponse;
import com.restaurant.store.dto.response.CartResponse;
import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.dto.response.OrderStatusMessage;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
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
    private final PickupRepository pickupRepository;
    private final JwtUtil jwtUtil;
    private final OrderMapper orderMapper;
    private final PaymentService paymentService;
    private final AdminIntegrationService adminIntegrationService;
    private final CartService cartService;
    private final OrderStatusWebSocketController orderStatusWebSocketController;

    private static final EnumSet<PaymentStatus> REUSABLE_PAYMENT_STATUSES =
            EnumSet.of(PaymentStatus.PENDING, PaymentStatus.PROCESSING);
    private static final Duration DEFAULT_PREPARATION_DURATION = Duration.ofMinutes(30);
    private static final Duration PICKUP_WINDOW_PADDING = Duration.ofMinutes(10);
    private static final Duration PICKUP_WINDOW_DURATION = Duration.ofMinutes(30);

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String token) {
        // Extract customer from token
        String email = jwtUtil.extractUsername(token.substring(7));
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        validateOrderDetails(request);

        // Validate and calculate total price
        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderItemRequest> requestedItems = resolveOrderItems(request, customer);

        for (OrderItemRequest itemRequest : requestedItems) {
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
        if (request.getOrderType() == OrderType.DELIVERY) {
            order.setDeliveryAddress(request.getDeliveryAddress());
        } else {
            order.setDeliveryAddress(null);
        }
        order.setPhoneNumber(request.getPhoneNumber());
        order.setSpecialInstructions(request.getSpecialInstructions());
        order.setStatus(OrderStatus.PENDING);
        order.setEstimatedDeliveryTime(LocalDateTime.now().plus(DEFAULT_PREPARATION_DURATION));

        order = orderRepository.save(order);

        // Link order items to order
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        orderItemRepository.saveAll(orderItems);

        // Create delivery or pickup record depending on order type
        Pickup pickup = null;
        if (request.getOrderType() == OrderType.DELIVERY) {
            Delivery delivery = new Delivery();
            delivery.setOrder(order);
            delivery.setDeliveryAddress(request.getDeliveryAddress());
            delivery.setPhoneNumber(request.getPhoneNumber());
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setEstimatedDeliveryTime(order.getEstimatedDeliveryTime());
            deliveryRepository.save(delivery);
        } else if (request.getOrderType() == OrderType.PICKUP) {
            pickup = createPickupRecord(order, request, customer);
        }

        // Sync order to Admin backend
        try {
            log.info("Syncing order {} to Admin backend", order.getId());
            adminIntegrationService.syncOrderToAdmin(order.getId());
        } catch (Exception e) {
            log.error("Failed to sync order to Admin backend", e);
        }

        List<OrderItem> persistedItems = orderItemRepository.findByOrderId(order.getId());
        OrderResponse response = orderMapper.toResponse(order, persistedItems);
        orderStatusWebSocketController.sendOrderUpdate(order.getId(), response);
        publishStatusUpdate(order,
                "ORDER_CREATED",
                "Order placed",
                "We've received your order and it's now pending.",
                Map.of("itemCount", orderItems.size()),
                pickup);
        return response;
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
            return paymentService.createPayment(order)
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
                    .findFirstByOrderIdAndMethodAndStatusInOrderByUpdatedAtDesc(
                            order.getId(),
                            request.getPaymentMethod(),
                            REUSABLE_PAYMENT_STATUSES
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
            order = orderRepository.save(order);
            updatePickupStatus(order, PickupStatus.PREPARING);

            broadcastOrderSnapshot(order, null);
            publishStatusUpdate(order,
                    "PAYMENT_CONFIRMED",
                    "Payment confirmed",
                    "We've confirmed your payment and the kitchen will start preparing your order.",
                    Map.of(
                            "paymentMethod", request.getPaymentMethod().name(),
                            "paymentStatus", PaymentStatus.CASH_PENDING.name()
                    ),
                    null);

            return "Payment processed successfully";
        }

        // For Stripe payments, confirm the payment intent
        if (!"intent".equalsIgnoreCase(paymentService.getServiceType())) {
            throw new BadRequestException("Payment intent flow is disabled for the current configuration");
        }

        try {
            paymentService.handlePaymentSuccess(request.getTransactionId());
            order.setStatus(OrderStatus.CONFIRMED);
            order = orderRepository.save(order);
            updatePickupStatus(order, PickupStatus.PREPARING);

            broadcastOrderSnapshot(order, null);
            publishStatusUpdate(order,
                    "PAYMENT_CONFIRMED",
                    "Payment confirmed",
                    "Stripe payment completed successfully.",
                    Map.of(
                            "paymentMethod", request.getPaymentMethod().name(),
                            "transactionId", request.getTransactionId()
                    ),
                    null);
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
        updatePickupStatus(order, PickupStatus.CANCELLED);

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        OrderResponse response = enrichOrderResponse(orderMapper.toResponse(order, orderItems), order);
        orderStatusWebSocketController.sendOrderUpdate(orderId, response);
        publishStatusUpdate(order,
                "ORDER_CANCELLED",
                "Order cancelled",
                "Your order has been cancelled successfully.",
                Map.of("cancelledBy", "CUSTOMER"),
                null);
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

        if (order.getOrderType() == OrderType.PICKUP) {
            pickupRepository.findByOrderId(order.getId()).ifPresent(pickup -> {
                response.setPickupStatus(pickup.getStatus());
                response.setPickupCode(pickup.getPickupCode());
                response.setPickupReadyAt(pickup.getReadyAt());
                response.setPickupWindowStart(pickup.getWindowStart());
                response.setPickupWindowEnd(pickup.getWindowEnd());
                response.setPickupPickedUpAt(pickup.getPickedUpAt());
                response.setPickupInstructions(pickup.getInstructions());
            });
        }

        return response;
    }

    private Customer getCustomerFromToken(String token) {
        String email = jwtUtil.extractUsername(token.substring(7));
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private void broadcastOrderSnapshot(Order order, List<OrderItem> orderItems) {
        if (order == null) {
            return;
        }

        List<OrderItem> items = orderItems != null ? orderItems : orderItemRepository.findByOrderId(order.getId());
        OrderResponse response = enrichOrderResponse(orderMapper.toResponse(order, items), order);
        orderStatusWebSocketController.sendOrderUpdate(order.getId(), response);
    }

    private void publishStatusUpdate(Order order,
                                     String eventType,
                                     String title,
                                     String message,
                                     Map<String, Object> metadata,
                                     Pickup pickupOverride) {
        if (order == null) {
            return;
        }

        OrderStatusMessage statusMessage = OrderStatusMessage.builder()
                .status(order.getStatus().name())
                .eventType(eventType)
                .title(title)
                .message(message)
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .metadata(buildOrderMetadata(order, metadata, pickupOverride))
                .build();

        orderStatusWebSocketController.sendOrderStatusUpdate(order.getId(), statusMessage);
        orderStatusWebSocketController.sendOrderNotification(order.getId(), statusMessage);
    }

    private List<OrderItemRequest> resolveOrderItems(CreateOrderRequest request, Customer customer) {
        if (request.getOrderItems() != null && !request.getOrderItems().isEmpty()) {
            return request.getOrderItems();
        }

        CartResponse cartResponse = cartService.getCartByCustomerId(customer.getId());
        List<CartItemResponse> cartItems = cartResponse.getItems();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BadRequestException("Order items cannot be empty");
        }

        return cartItems.stream()
                .map(item -> new OrderItemRequest(item.getProductId(), item.getQuantity(), null))
                .collect(Collectors.toList());
    }

    private void validateOrderDetails(CreateOrderRequest request) {
        if (request == null || request.getOrderType() == null) {
            throw new BadRequestException("Order type is required");
        }

        if (request.getOrderType() == OrderType.DELIVERY) {
            if (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank()) {
                throw new BadRequestException("Delivery address is required for delivery orders");
            }
            if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
                throw new BadRequestException("Phone number is required for delivery orders");
            }
        } else if (request.getOrderType() == OrderType.PICKUP) {
            if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
                throw new BadRequestException("Phone number is required for pickup orders");
            }
        }
    }

    private Pickup createPickupRecord(Order order, CreateOrderRequest request, Customer customer) {
        Pickup pickup = new Pickup();
        pickup.setOrder(order);
        pickup.setPickupCode(generatePickupCode());
        pickup.setStatus(PickupStatus.AWAITING_CONFIRMATION);
        pickup.setInstructions(request.getSpecialInstructions());
        pickup.setContactName(customer.getName());
        pickup.setContactPhone(order.getPhoneNumber());

        LocalDateTime readyAt = order.getEstimatedDeliveryTime();
        pickup.setReadyAt(readyAt);
        if (readyAt != null) {
            pickup.setWindowStart(readyAt.minus(PICKUP_WINDOW_PADDING));
            pickup.setWindowEnd(readyAt.plus(PICKUP_WINDOW_DURATION));
        }

        return pickupRepository.save(pickup);
    }

    private void updatePickupStatus(Order order, PickupStatus newStatus) {
        if (order == null || order.getOrderType() != OrderType.PICKUP || newStatus == null) {
            return;
        }

        pickupRepository.findByOrderId(order.getId()).ifPresent(pickup -> {
            pickup.setStatus(newStatus);
            if (newStatus == PickupStatus.PREPARING && order.getEstimatedDeliveryTime() != null) {
                LocalDateTime readyAt = order.getEstimatedDeliveryTime();
                pickup.setReadyAt(readyAt);
                pickup.setWindowStart(readyAt.minus(PICKUP_WINDOW_PADDING));
                pickup.setWindowEnd(readyAt.plus(PICKUP_WINDOW_DURATION));
            }
            if (newStatus == PickupStatus.CANCELLED) {
                pickup.setWindowEnd(LocalDateTime.now());
            }
            pickupRepository.save(pickup);
        });
    }

    private String generatePickupCode() {
        return "PU-" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8).toUpperCase();
    }

    private Map<String, Object> buildOrderMetadata(Order order,
                                                   Map<String, Object> metadata,
                                                   Pickup pickupOverride) {
        Map<String, Object> enrichedMetadata = new HashMap<>();
        if (metadata != null) {
            enrichedMetadata.putAll(metadata);
        }

        enrichedMetadata.put("orderId", order.getId());
        enrichedMetadata.put("totalPrice", order.getTotalPrice());
        enrichedMetadata.put("orderType", order.getOrderType().name());

        if (order.getOrderType() == OrderType.PICKUP) {
            Pickup pickup = pickupOverride;
            if (pickup == null) {
                pickup = pickupRepository.findByOrderId(order.getId()).orElse(null);
            }
            if (pickup != null) {
                enrichedMetadata.put("pickupCode", pickup.getPickupCode());
                enrichedMetadata.put("pickupStatus", pickup.getStatus().name());
                enrichedMetadata.put("pickupWindowStart", pickup.getWindowStart());
                enrichedMetadata.put("pickupWindowEnd", pickup.getWindowEnd());
            }
        }

        return enrichedMetadata;
    }
}

