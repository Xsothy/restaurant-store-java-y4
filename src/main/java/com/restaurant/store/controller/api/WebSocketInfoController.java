package com.restaurant.store.controller.api;

import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.websocket.WebSocketDocumentation;
import com.restaurant.store.service.WebSocketDocumentationService;
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
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class WebSocketInfoController {

    private final WebSocketDocumentationService documentationService;

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
    public ResponseEntity<ApiResponse<WebSocketDocumentation>> getWebSocketInfo() {
        WebSocketDocumentation info = documentationService.getDocumentation();

        return ResponseEntity.ok(ApiResponse.success(
                "WebSocket connection information retrieved successfully",
                info
        ));
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
        private WebSocketDocumentation data;
    }
}
