package com.restaurant.store.controller.api;

import com.restaurant.store.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controller that provides information about available WebSocket endpoints.
 * Since WebSocket endpoints don't appear in standard OpenAPI documentation,
 * this controller serves as a REST endpoint to document WebSocket functionality.
 */
@RestController
@RequestMapping("/api/websocket")
@CrossOrigin(origins = "*")
@Tag(name = "WebSocket Information", description = "Documentation for WebSocket endpoints and real-time features")
public class WebSocketInfoController {

    @GetMapping("/info")
    @Operation(
        summary = "Get WebSocket Connection Information",
        description = """
            Returns information about how to connect to WebSocket endpoints for real-time updates.
            
            The application uses STOMP over WebSocket with SockJS fallback.
            
            **Connection Steps:**
            1. Create SockJS connection to `/ws` endpoint
            2. Create STOMP client over the SockJS connection
            3. Connect to STOMP broker
            4. Subscribe to relevant topics
            5. Optionally send subscription messages to receive confirmation
            
            **Available Topics:**
            - Order Status: `/topic/orders/{orderId}`, `/topic/orders/{orderId}/status`, `/topic/orders/{orderId}/notifications`
            - Delivery Tracking: `/topic/deliveries/{orderId}`, `/topic/deliveries/{orderId}/status`, `/topic/deliveries/{orderId}/location`, `/topic/deliveries/{orderId}/notifications`
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "WebSocket connection information retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WebSocketInfoResponse.class),
                examples = @ExampleObject(
                    name = "WebSocket Info Example",
                    value = """
                        {
                          "success": true,
                          "message": "WebSocket connection information",
                          "data": {
                            "endpoint": "ws://localhost:8080/ws",
                            "protocol": "STOMP over SockJS",
                            "topics": [
                              {
                                "topic": "/topic/orders/{orderId}",
                                "description": "Order status updates",
                                "subscribeEndpoint": "/app/orders/{orderId}/subscribe",
                                "messageTypes": ["OrderResponse", "OrderStatusMessage"]
                              },
                              {
                                "topic": "/topic/deliveries/{orderId}",
                                "description": "Delivery tracking updates",
                                "subscribeEndpoint": "/app/deliveries/{orderId}/subscribe",
                                "messageTypes": ["DeliveryResponse", "OrderStatusMessage"]
                              }
                            ],
                            "exampleCode": {
                              "javascript": "const socket = new SockJS('http://localhost:8080/ws');\\nconst stompClient = Stomp.over(socket);\\nstompClient.connect({}, () => {\\n    stompClient.subscribe('/topic/orders/123', (message) => {\\n        console.log('Order update:', JSON.parse(message.body));\\n    });\\n});"
                            }
                          }
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<ApiResponse<WebSocketInfo>> getWebSocketInfo() {
        WebSocketInfo info = WebSocketInfo.builder()
                .endpoint("ws://localhost:8080/ws")
                .protocol("STOMP over SockJS")
                .topics(List.of(
                        WebSocketTopicInfo.builder()
                                .topic("/topic/orders/{orderId}")
                                .description("Real-time order status updates")
                                .subscribeEndpoint("/app/orders/{orderId}/subscribe")
                                .messageTypes(List.of("OrderResponse", "OrderStatusMessage"))
                                .build(),
                        WebSocketTopicInfo.builder()
                                .topic("/topic/orders/{orderId}/status")
                                .description("Detailed order status messages")
                                .subscribeEndpoint("Subscribe to /topic/orders/{orderId}")
                                .messageTypes(List.of("OrderStatusMessage"))
                                .build(),
                        WebSocketTopicInfo.builder()
                                .topic("/topic/orders/{orderId}/notifications")
                                .description("Order notifications and alerts")
                                .subscribeEndpoint("Subscribe to /topic/orders/{orderId}")
                                .messageTypes(List.of("OrderStatusMessage"))
                                .build(),
                        WebSocketTopicInfo.builder()
                                .topic("/topic/deliveries/{orderId}")
                                .description("Real-time delivery tracking updates")
                                .subscribeEndpoint("/app/deliveries/{orderId}/subscribe")
                                .messageTypes(List.of("DeliveryResponse", "OrderStatusMessage"))
                                .build(),
                        WebSocketTopicInfo.builder()
                                .topic("/topic/deliveries/{orderId}/status")
                                .description("Detailed delivery status messages")
                                .subscribeEndpoint("Subscribe to /topic/deliveries/{orderId}")
                                .messageTypes(List.of("OrderStatusMessage"))
                                .build(),
                        WebSocketTopicInfo.builder()
                                .topic("/topic/deliveries/{orderId}/location")
                                .description("Real-time delivery location updates")
                                .subscribeEndpoint("Subscribe to /topic/deliveries/{orderId}")
                                .messageTypes(List.of("OrderStatusMessage with location data"))
                                .build(),
                        WebSocketTopicInfo.builder()
                                .topic("/topic/deliveries/{orderId}/notifications")
                                .description("Delivery notifications (driver assigned, arriving soon, etc.)")
                                .subscribeEndpoint("Subscribe to /topic/deliveries/{orderId}")
                                .messageTypes(List.of("OrderStatusMessage"))
                                .build()
                ))
                .exampleCode(Map.of(
                        "javascript", """
                                // Using SockJS and STOMP.js
                                const socket = new SockJS('http://localhost:8080/ws');
                                const stompClient = Stomp.over(socket);
                                
                                stompClient.connect({}, () => {
                                    console.log('Connected to WebSocket');
                                    
                                    // Subscribe to order updates
                                    stompClient.subscribe('/topic/orders/123', (message) => {
                                        const orderUpdate = JSON.parse(message.body);
                                        console.log('Order update:', orderUpdate);
                                    });
                                    
                                    // Subscribe to delivery tracking
                                    stompClient.subscribe('/topic/deliveries/123', (message) => {
                                        const deliveryUpdate = JSON.parse(message.body);
                                        console.log('Delivery update:', deliveryUpdate);
                                    });
                                    
                                    // Send subscription message (optional, triggers confirmation)
                                    stompClient.send('/app/deliveries/123/subscribe', {}, JSON.stringify({}));
                                });
                                
                                stompClient.onError = (error) => {
                                    console.error('WebSocket error:', error);
                                };
                                """,
                        "html", """
                                <!-- Include SockJS and STOMP libraries -->
                                <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
                                <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
                                """
                ))
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                "WebSocket connection information retrieved successfully", 
                info
        ));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "WebSocket connection and topic information")
    public static class WebSocketInfo {
        @Schema(description = "WebSocket endpoint URL", example = "ws://localhost:8080/ws")
        private String endpoint;

        @Schema(description = "Protocol used", example = "STOMP over SockJS")
        private String protocol;

        @Schema(description = "Available WebSocket topics")
        private List<WebSocketTopicInfo> topics;

        @Schema(description = "Example code for connecting to WebSocket")
        private Map<String, String> exampleCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Information about a specific WebSocket topic")
    public static class WebSocketTopicInfo {
        @Schema(description = "Topic path", example = "/topic/deliveries/{orderId}")
        private String topic;

        @Schema(description = "Description of what this topic provides", 
                example = "Real-time delivery tracking updates")
        private String description;

        @Schema(description = "Endpoint to send subscription message (if applicable)", 
                example = "/app/deliveries/{orderId}/subscribe")
        private String subscribeEndpoint;

        @Schema(description = "Types of messages sent on this topic", 
                example = "[\"DeliveryResponse\", \"OrderStatusMessage\"]")
        private List<String> messageTypes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Wrapper for WebSocket info response")
    public static class WebSocketInfoResponse {
        @Schema(description = "Success status")
        private boolean success;

        @Schema(description = "Response message")
        private String message;

        @Schema(description = "WebSocket connection information")
        private WebSocketInfo data;
    }
}
