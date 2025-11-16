package com.restaurant.store.dto.websocket;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "WebSocket connection and topic information")
public class WebSocketDocumentation {

    @Schema(description = "WebSocket endpoint URL", example = "ws://localhost:8080/ws")
    private String endpoint;

    @Schema(description = "Protocol used", example = "STOMP over SockJS")
    private String protocol;

    @Schema(description = "Available WebSocket topics")
    private List<WebSocketTopic> topics;

    @Schema(description = "Example code for connecting to WebSocket")
    private Map<String, String> exampleCode;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Information about a specific WebSocket topic")
    public static class WebSocketTopic {

        @Schema(description = "Topic path", example = "/topic/deliveries/{orderId}")
        private String topic;

        @Schema(description = "Description of what this topic provides",
                example = "Real-time delivery tracking updates")
        private String description;

        @Schema(description = "Endpoint to send subscription message (if applicable)",
                example = "/app/api/deliveries/{orderId}/subscribe")
        private String subscribeEndpoint;

        @Schema(description = "Types of messages sent on this topic",
                example = "[\"DeliveryResponse\", \"OrderStatusMessage\"]")
        private List<String> messageTypes;
    }
}
