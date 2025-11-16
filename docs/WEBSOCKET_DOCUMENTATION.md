# WebSocket Documentation

## Overview

The Restaurant Store API provides real-time updates through WebSocket connections using STOMP (Simple Text Oriented Messaging Protocol) over SockJS.

## Connection Details

- **WebSocket Endpoint:** `ws://localhost:8080/ws` (or `wss://` for HTTPS)
- **Protocol:** STOMP over SockJS
- **Fallback:** SockJS provides fallback options when WebSocket is unavailable

## Important Note About URL Confusion

⚠️ **Common Mistake:**
- ❌ DO NOT try to connect to `/api/deliveries/track/506` as a WebSocket - this is a REST endpoint
- ✅ Instead, connect to `/ws` and subscribe to `/topic/deliveries/506`

### REST vs WebSocket Endpoints

| Feature | REST Endpoint (Polling) | WebSocket Topic (Real-time) |
|---------|------------------------|----------------------------|
| Order Tracking | `GET /api/orders/{id}` | `/topic/orders/{orderId}` |
| Delivery Tracking | `GET /api/deliveries/track/{orderId}` | `/topic/deliveries/{orderId}` |
| Connection Type | HTTP Request/Response | Persistent WebSocket |
| Updates | Manual polling required | Automatic push notifications |

## Available Topics

### Order Status Tracking

| Topic | Description | Message Type |
|-------|-------------|--------------|
| `/topic/orders/{orderId}` | Full order updates | `OrderResponse` |
| `/topic/orders/{orderId}/status` | Status change messages | `OrderStatusMessage` |
| `/topic/orders/{orderId}/notifications` | Order notifications | `OrderStatusMessage` |

### Delivery Tracking

| Topic | Description | Message Type |
|-------|-------------|--------------|
| `/topic/deliveries/{orderId}` | Full delivery updates | `DeliveryResponse` |
| `/topic/deliveries/{orderId}/status` | Status change messages | `OrderStatusMessage` |
| `/topic/deliveries/{orderId}/location` | Real-time location updates | `OrderStatusMessage` |
| `/topic/deliveries/{orderId}/notifications` | Delivery notifications | `OrderStatusMessage` |

## Subscription Endpoints

To receive a confirmation message when subscribing, send a message to:

- **Order Subscription:** `/app/orders/{orderId}/subscribe`
- **Delivery Subscription:** `/app/deliveries/{orderId}/subscribe`

These will respond on the corresponding `/topic/*` channel with a subscription confirmation.

## Connection Examples

### JavaScript (Browser)

#### 1. Include Required Libraries

```html
<!-- SockJS -->
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<!-- STOMP.js -->
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
```

#### 2. Connect and Subscribe to Delivery Tracking

```javascript
// Create SockJS connection
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// Optional: Enable debug logging
// stompClient.debug = (str) => console.log('STOMP:', str);

// Connect
stompClient.connect({}, function(frame) {
    console.log('Connected to WebSocket:', frame);
    
    // Subscribe to delivery tracking for order 506
    stompClient.subscribe('/topic/deliveries/506', function(message) {
        const deliveryUpdate = JSON.parse(message.body);
        console.log('Delivery Update:', deliveryUpdate);
        
        // Update UI with delivery information
        updateDeliveryStatus(deliveryUpdate);
    });
    
    // Subscribe to location updates
    stompClient.subscribe('/topic/deliveries/506/location', function(message) {
        const locationUpdate = JSON.parse(message.body);
        console.log('Location Update:', locationUpdate);
        
        // Update map/location in UI
        updateDeliveryLocation(locationUpdate);
    });
    
    // Send subscription message (optional, triggers confirmation)
    stompClient.send('/app/deliveries/506/subscribe', {}, JSON.stringify({}));
    
}, function(error) {
    console.error('WebSocket connection error:', error);
});

// Helper function to update UI
function updateDeliveryStatus(delivery) {
    document.getElementById('delivery-status').textContent = delivery.status;
    document.getElementById('delivery-location').textContent = delivery.location;
}

function updateDeliveryLocation(locationMessage) {
    // Update map marker or location display
    document.getElementById('current-location').textContent = locationMessage.status;
}
```

#### 3. Connect and Subscribe to Order Status

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected:', frame);
    
    // Subscribe to order updates
    const orderId = 506;
    stompClient.subscribe(`/topic/orders/${orderId}`, function(message) {
        const order = JSON.parse(message.body);
        console.log('Order Update:', order);
        
        // Update order display
        document.getElementById('order-status').textContent = order.status;
    });
    
    // Subscribe to order notifications
    stompClient.subscribe(`/topic/orders/${orderId}/notifications`, function(message) {
        const notification = JSON.parse(message.body);
        console.log('Notification:', notification);
        
        // Show notification to user
        showNotification(notification.title, notification.message);
    });
    
    // Confirm subscription
    stompClient.send(`/app/orders/${orderId}/subscribe`, {}, JSON.stringify({}));
});
```

#### 4. Disconnect

```javascript
// Disconnect when leaving page
window.addEventListener('beforeunload', () => {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect(() => {
            console.log('Disconnected from WebSocket');
        });
    }
});
```

### Complete Example with Error Handling

```javascript
class DeliveryTracker {
    constructor(orderId) {
        this.orderId = orderId;
        this.stompClient = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
    }
    
    connect() {
        const socket = new SockJS('http://localhost:8080/ws');
        this.stompClient = Stomp.over(socket);
        
        // Disable debug in production
        this.stompClient.debug = null;
        
        this.stompClient.connect(
            {},
            this.onConnected.bind(this),
            this.onError.bind(this)
        );
    }
    
    onConnected(frame) {
        console.log('Connected to delivery tracking');
        this.reconnectAttempts = 0;
        
        // Subscribe to all relevant topics
        this.stompClient.subscribe(
            `/topic/deliveries/${this.orderId}`,
            this.onDeliveryUpdate.bind(this)
        );
        
        this.stompClient.subscribe(
            `/topic/deliveries/${this.orderId}/location`,
            this.onLocationUpdate.bind(this)
        );
        
        this.stompClient.subscribe(
            `/topic/deliveries/${this.orderId}/notifications`,
            this.onNotification.bind(this)
        );
        
        // Send subscription confirmation
        this.stompClient.send(
            `/app/deliveries/${this.orderId}/subscribe`,
            {},
            JSON.stringify({})
        );
    }
    
    onDeliveryUpdate(message) {
        const delivery = JSON.parse(message.body);
        console.log('Delivery update:', delivery);
        
        // Update UI
        this.updateDeliveryDisplay(delivery);
    }
    
    onLocationUpdate(message) {
        const location = JSON.parse(message.body);
        console.log('Location update:', location);
        
        // Update map
        this.updateMap(location);
    }
    
    onNotification(message) {
        const notification = JSON.parse(message.body);
        console.log('Notification:', notification);
        
        // Show notification
        this.showNotification(notification);
    }
    
    onError(error) {
        console.error('WebSocket error:', error);
        
        // Attempt reconnection
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
            console.log(`Reconnecting in ${delay}ms...`);
            
            setTimeout(() => {
                this.connect();
            }, delay);
        } else {
            console.error('Max reconnection attempts reached');
            this.showError('Unable to connect to real-time tracking. Please refresh the page.');
        }
    }
    
    disconnect() {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.disconnect(() => {
                console.log('Disconnected from delivery tracking');
            });
        }
    }
    
    updateDeliveryDisplay(delivery) {
        // Implement UI update logic
        document.getElementById('delivery-status').textContent = delivery.status;
    }
    
    updateMap(location) {
        // Implement map update logic
        console.log('Update map to:', location.status);
    }
    
    showNotification(notification) {
        // Implement notification display
        if (Notification.permission === 'granted') {
            new Notification(notification.title, {
                body: notification.message
            });
        }
    }
    
    showError(message) {
        // Implement error display
        alert(message);
    }
}

// Usage
const tracker = new DeliveryTracker(506);
tracker.connect();

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
    tracker.disconnect();
});
```

## Testing WebSocket Connection

### Using Browser Console

```javascript
// Test connection in browser console
var socket = new SockJS('http://localhost:8080/ws');
var stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected!', frame);
    
    // Subscribe to test order
    stompClient.subscribe('/topic/orders/1', function(msg) {
        console.log('Received:', JSON.parse(msg.body));
    });
});
```

### Using curl (to trigger updates)

```bash
# Update order status (triggers WebSocket broadcast)
curl -X POST http://localhost:8080/api/internal/orders/506/status \
  -H "Content-Type: application/json" \
  -d '{"status": "OUT_FOR_DELIVERY"}'
```

## Message Types

### OrderResponse
```json
{
  "id": 506,
  "customerId": 123,
  "status": "OUT_FOR_DELIVERY",
  "totalAmount": 45.99,
  "items": [...],
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T11:00:00"
}
```

### OrderStatusMessage
```json
{
  "orderId": 506,
  "eventType": "STATUS_CHANGED",
  "status": "OUT_FOR_DELIVERY",
  "title": "Order Out for Delivery",
  "message": "Your order is on its way!",
  "timestamp": "2024-01-15T11:00:00"
}
```

### DeliveryResponse
```json
{
  "id": 789,
  "orderId": 506,
  "status": "OUT_FOR_DELIVERY",
  "location": "123 Main St, City",
  "estimatedDeliveryTime": "2024-01-15T11:30:00",
  "driverName": "John Doe",
  "driverPhone": "+1234567890"
}
```

## Best Practices

1. **Always use SockJS**: It provides fallback mechanisms for environments where WebSocket isn't available
2. **Handle reconnection**: Implement exponential backoff for reconnection attempts
3. **Subscribe after connection**: Only subscribe to topics after the connection is established
4. **Unsubscribe on cleanup**: Always disconnect when leaving the page
5. **Use meaningful logging**: Enable debug mode during development, disable in production
6. **Handle errors gracefully**: Provide fallback to REST API polling if WebSocket fails
7. **Optimize subscriptions**: Only subscribe to topics you need
8. **Security**: In production, implement proper authentication for WebSocket connections

## Troubleshooting

### Error: "Connection was not upgraded to WebSocket"

This error occurs when trying to connect to a REST endpoint as a WebSocket.

**Problem:** Connecting to `/api/deliveries/track/506` as WebSocket
**Solution:** Connect to `/ws` and subscribe to `/topic/deliveries/506`

```javascript
// ❌ Wrong
const socket = new SockJS('http://localhost:8080/api/deliveries/track/506');

// ✅ Correct
const socket = new SockJS('http://localhost:8080/ws');
stompClient.subscribe('/topic/deliveries/506', callback);
```

### Connection Fails Immediately

1. **Check server is running**: Ensure Spring Boot application is running
2. **Check WebSocket endpoint**: Verify `/ws` endpoint is accessible
3. **Check CORS**: Ensure WebSocket origins are allowed in `application.properties`
4. **Check browser console**: Look for detailed error messages

### No Messages Received

1. **Verify subscription**: Ensure you're subscribed to the correct topic
2. **Check topic format**: Topic should be `/topic/deliveries/{orderId}` not `/app/deliveries/{orderId}`
3. **Trigger an update**: Use internal API or admin panel to trigger a status change
4. **Check message callback**: Ensure your callback function is correctly defined

## API Documentation

For a complete list of WebSocket topics and connection details via REST API:

```bash
curl http://localhost:8080/api/websocket/info
```

Or visit: `http://localhost:8080/swagger-ui.html` and look for the "WebSocket Information" section.

## Production Considerations

1. **Use WSS**: In production, use `wss://` (WebSocket Secure) instead of `ws://`
2. **Authentication**: Implement token-based authentication for WebSocket connections
3. **Rate Limiting**: Implement rate limiting to prevent abuse
4. **Monitoring**: Monitor WebSocket connection metrics
5. **Scaling**: Use message broker (Redis, RabbitMQ) for horizontal scaling
6. **CORS**: Configure allowed origins properly for production domains
