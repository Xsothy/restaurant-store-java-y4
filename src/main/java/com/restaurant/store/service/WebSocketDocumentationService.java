package com.restaurant.store.service;

import com.restaurant.store.dto.websocket.WebSocketDocumentation;
import com.restaurant.store.dto.websocket.WebSocketDocumentation.WebSocketTopic;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WebSocketDocumentationService {

    public WebSocketDocumentation getDocumentation() {
        List<WebSocketTopic> topics = List.of(
                WebSocketTopic.builder()
                        .topic("/topic/orders/{orderId}")
                        .description("Real-time order status updates")
                        .subscribeEndpoint("/app/api/orders/{orderId}/subscribe")
                        .messageTypes(List.of("OrderResponse", "OrderStatusMessage"))
                        .build(),
                WebSocketTopic.builder()
                        .topic("/topic/orders/{orderId}/status")
                        .description("Detailed order status messages")
                        .subscribeEndpoint("Subscribe to /topic/orders/{orderId}")
                        .messageTypes(List.of("OrderStatusMessage"))
                        .build(),
                WebSocketTopic.builder()
                        .topic("/topic/orders/{orderId}/notifications")
                        .description("Order notifications and alerts")
                        .subscribeEndpoint("Subscribe to /topic/orders/{orderId}")
                        .messageTypes(List.of("OrderStatusMessage"))
                        .build(),
                WebSocketTopic.builder()
                        .topic("/topic/deliveries/{orderId}")
                        .description("Real-time delivery tracking updates")
                        .subscribeEndpoint("/app/api/deliveries/{orderId}/subscribe")
                        .messageTypes(List.of("DeliveryResponse", "OrderStatusMessage"))
                        .build(),
                WebSocketTopic.builder()
                        .topic("/topic/deliveries/{orderId}/status")
                        .description("Detailed delivery status messages")
                        .subscribeEndpoint("Subscribe to /topic/deliveries/{orderId}")
                        .messageTypes(List.of("OrderStatusMessage"))
                        .build(),
                WebSocketTopic.builder()
                        .topic("/topic/deliveries/{orderId}/location")
                        .description("Real-time delivery location updates")
                        .subscribeEndpoint("Subscribe to /topic/deliveries/{orderId}")
                        .messageTypes(List.of("OrderStatusMessage with location data"))
                        .build(),
                WebSocketTopic.builder()
                        .topic("/topic/deliveries/{orderId}/notifications")
                        .description("Delivery notifications (driver assigned, arriving soon, etc.)")
                        .subscribeEndpoint("Subscribe to /topic/deliveries/{orderId}")
                        .messageTypes(List.of("OrderStatusMessage"))
                        .build()
        );

        Map<String, String> exampleCode = Map.of(
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
                            stompClient.send('/app/api/deliveries/123/subscribe', {}, JSON.stringify({}));
                        });

                        stompClient.onError = (error) => {
                            console.error('WebSocket error:', error);
                        };
                        """,
                "html", """
                        <!-- Include SockJS and STOMP libraries -->
                        <script src=\"https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js\"></script>
                        <script src=\"https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js\"></script>
                        """
        );

        return WebSocketDocumentation.builder()
                .endpoint("ws://localhost:8080/ws")
                .protocol("STOMP over SockJS")
                .topics(topics)
                .exampleCode(exampleCode)
                .build();
    }
}
