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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides a short, OpenAPI-friendly endpoint that documents WebSocket usage at /api/ws.
 * This mirrors the /api/websocket/info endpoint so the information is also available
 * at the concise path requested by clients.
 */
@RestController
@RequestMapping("/api/ws")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Tag(name = "WebSocket Documentation", description = "Provides OpenAPI documentation for WebSocket endpoints")
public class WebSocketDocumentationController {

    private final WebSocketDocumentationService documentationService;

    @GetMapping
    @Operation(
            summary = "Get WebSocket Documentation",
            description = "Returns WebSocket connection details so that /api/ws exists in the OpenAPI document."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "WebSocket documentation retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = WebSocketInfoController.WebSocketInfoResponse.class),
                            examples = @ExampleObject(
                                    name = "WebSocket Documentation",
                                    value = """
                                            {
                                              "success": true,
                                              "message": "WebSocket documentation",
                                              "data": {
                                                "endpoint": "ws://localhost:8080/ws",
                                                "protocol": "STOMP over SockJS",
                                                "topics": [
                                                  {
                                                    "topic": "/topic/orders/{orderId}",
                                                    "description": "Real-time order status updates",
                                                    "subscribeEndpoint": "/app/orders/{orderId}/subscribe",
                                                    "messageTypes": ["OrderResponse", "OrderStatusMessage"]
                                                  }
                                                ],
                                                "exampleCode": {
                                                  "javascript": "const socket = new SockJS('http://localhost:8080/ws');",
                                                  "html": "<script src=\\"https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js\\"></script>"
                                                }
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<ApiResponse<WebSocketDocumentation>> getWebSocketDocumentation() {
        WebSocketDocumentation documentation = documentationService.getDocumentation();
        return ResponseEntity.ok(ApiResponse.success("WebSocket documentation", documentation));
    }
}
