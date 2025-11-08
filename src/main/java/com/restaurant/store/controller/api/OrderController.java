package com.restaurant.store.controller.api;

import com.restaurant.store.dto.request.CreateOrderRequest;
import com.restaurant.store.dto.request.PaymentRequest;
import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.ErrorResponse;
import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@Tag(name = "Orders", description = "Order management and processing endpoints")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Operation(
            summary = "Create order",
            description = "Creates a new order from the current user's cart items"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Order created successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid order data or empty cart",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("Authorization") String authToken) {
        
        OrderResponse response = orderService.createOrder(request, authToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", response));
    }
    
    @Operation(
            summary = "Get order by ID",
            description = "Retrieves details of a specific order"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        OrderResponse response = orderService.getOrderById(orderId, authToken);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", response));
    }
    
    @Operation(
            summary = "Get order status",
            description = "Retrieves the current status of an order"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order status retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<String>> getOrderStatus(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        String status = orderService.getOrderStatus(orderId, authToken);
        return ResponseEntity.ok(ApiResponse.success("Order status retrieved successfully", status));
    }
    
    @Operation(
            summary = "Create payment intent",
            description = "Creates a Stripe payment intent for order payment"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Payment intent created successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/{orderId}/payment-intent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPaymentIntent(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        Map<String, Object> paymentIntent = orderService.createPaymentIntent(orderId, authToken);
        return ResponseEntity.ok(ApiResponse.success("Payment intent created successfully", paymentIntent));
    }
    
    @Operation(
            summary = "Process payment",
            description = "Processes payment for an order (supports Stripe, Cash, Credit/Debit card)"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Payment processed successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid payment data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<ApiResponse<String>> payOrder(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("Authorization") String authToken) {
        
        String result = orderService.processPayment(orderId, request, authToken);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }
    
    @Operation(
            summary = "Get customer orders",
            description = "Retrieves all orders for a specific customer"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Customer orders retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getCustomerOrders(
            @Parameter(description = "Customer ID") @PathVariable Long customerId,
            @RequestHeader("Authorization") String authToken) {
        
        List<OrderResponse> orders = orderService.getCustomerOrders(customerId, authToken);
        return ResponseEntity.ok(ApiResponse.success("Customer orders retrieved successfully", orders));
    }
    
    @Operation(
            summary = "Get my orders",
            description = "Retrieves all orders for the currently authenticated user"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Orders retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @RequestHeader("Authorization") String authToken) {
        
        List<OrderResponse> orders = orderService.getMyOrders(authToken);
        return ResponseEntity.ok(ApiResponse.success("Your orders retrieved successfully", orders));
    }
    
    @Operation(
            summary = "Cancel order",
            description = "Cancels an existing order"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Order cancelled successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Order cannot be cancelled",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        String result = orderService.cancelOrder(orderId, authToken);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }
}
