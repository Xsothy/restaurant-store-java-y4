package com.restaurant.store.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.store.controller.api.DeliveryStatusWebSocketController;
import com.restaurant.store.controller.api.OrderStatusWebSocketController;
import com.restaurant.store.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.ExecutionException;

@Configuration
@ConditionalOnProperty(name = "admin.api.websocket.bridge.enabled", havingValue = "true")
@Slf4j
public class RemoteBackendClientConfig {

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolTaskScheduler adminWebSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("admin-ws-heartbeat-");
        return scheduler;
    }

    @Bean(destroyMethod = "stop")
    public WebSocketStompClient stompClient(TaskScheduler taskScheduler) {
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient client = new WebSocketStompClient(webSocketClient);
        client.setMessageConverter(new MappingJackson2MessageConverter());
        client.setTaskScheduler(taskScheduler);
        client.setDefaultHeartbeat(new long[]{10000, 10000});
        return client;
    }

    @Bean
    public MyStompSessionHandler myStompSessionHandler(OrderRepository orderRepository,
                                                      OrderStatusWebSocketController orderStatusWebSocketController,
                                                      DeliveryStatusWebSocketController deliveryStatusWebSocketController,
                                                      ObjectMapper objectMapper,
                                                      WebSocketStompClient stompClient,
                                                      @Value("${admin.api.websocket.url}") String websocketUrl,
                                                      @Value("${admin.api.websocket.topic:/topic/admin/orders}") String subscriptionTopic,
                                                      @Value("${admin.api.websocket.delivery-topic:/topic/deliveries}") String deliverySubscriptionTopic) {
        return new MyStompSessionHandler(orderRepository,
                orderStatusWebSocketController,
                deliveryStatusWebSocketController,
                objectMapper,
                stompClient,
                websocketUrl,
                subscriptionTopic,
                deliverySubscriptionTopic);
    }

    @Bean(destroyMethod = "disconnect")
    public StompSession session(WebSocketStompClient stompClient,
                                MyStompSessionHandler sessionHandler,
                                @Value("${admin.api.websocket.url}") String websocketUrl) throws ExecutionException, InterruptedException {
        log.info("Connecting to Admin WebSocket endpoint {}", websocketUrl);
        return stompClient.connect(websocketUrl, sessionHandler).get();
    }
}
