# WebSocket Topics Complete Guide

## Overview

This document provides a comprehensive guide to all WebSocket topics available in the Restaurant Store API. The application uses **STOMP over SockJS** for real-time communication.

## Connection Information

- **Endpoint**: `ws://localhost:8080/ws` (or `wss://` for HTTPS)
- **Protocol**: STOMP over SockJS
- **Message Format**: JSON
- **Configuration**: See `WebSocketConfig.java`

## Quick Start

### 1. Include Required Libraries

```html
<!-- Include SockJS and STOMP libraries -->
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
```

### 2. Connect to WebSocket

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, (frame) => {
    console.log('Connected: ' + frame);
    
    // Subscribe to topics here
}, (error) => {
    console.error('Connection error:', error);
});
```

## Order Topics

### 1. `/topic/orders/{orderId}` - Full Order Updates

**Description**: Receive complete order data updates including all order details, items, and status changes.

**Message Types**: `OrderResponse`, `OrderStatusMessage`

**Subscribe Endpoint**: `/app/orders/{orderId}/subscribe`

**Example**:
```javascript
stompClient.subscribe('/topic/orders/123', (message) => {
    const orderUpdate = JSON.parse(message.body);
    console.log('Order update:', orderUpdate);
    
    // orderUpdate contains:
    // - id, customer, items, totalPrice, status, orderType, etc.
});

// Optional: Send subscription confirmation
stompClient.send('/app/orders/123/subscribe', {}, JSON.stringify({}));
```

**Sample Payload**:
```json
{
  "id": 123,
  "customerId": 456,
  "totalPrice": 45.99,
  "status": "PREPARING",
  "orderType": "DELIVERY",
  "items": [
    {
      "id": 1,
      "productName": "Pizza",
      "quantity": 2,
      "price": 12.99
    }
  ],
  "createdAt": "2025-01-16T10:30:00",
  "estimatedDeliveryTime": "2025-01-16T11:00:00"
}
```

### 2. `/topic/orders/{orderId}/status` - Order Status Messages

**Description**: Receive detailed status change messages with event types and metadata.

**Message Type**: `OrderStatusMessage`

**Example**:
```javascript
stompClient.subscribe('/topic/orders/123/status', (message) => {
    const statusUpdate = JSON.parse(message.body);
    console.log('Status change:', statusUpdate);
    
    // Display notification to user
    showNotification(statusUpdate.title, statusUpdate.message);
});
```

**Sample Payload**:
```json
{
  "orderId": 123,
  "eventType": "ORDER_STATUS_CHANGED",
  "status": "PREPARING",
  "title": "Order is being prepared",
  "message": "Your order is now being prepared by our kitchen",
  "timestamp": "2025-01-16T10:35:00",
  "estimatedDeliveryTime": "2025-01-16T11:00:00",
  "metadata": {
    "orderId": 123,
    "orderType": "DELIVERY"
  }
}
```

### 3. `/topic/orders/{orderId}/notifications` - Order Notifications

**Description**: Receive important order notifications and alerts.

**Message Type**: `OrderStatusMessage`

**Example**:
```javascript
stompClient.subscribe('/topic/orders/123/notifications', (message) => {
    const notification = JSON.parse(message.body);
    
    // Show toast notification
    showToast({
        title: notification.title,
        message: notification.message,
        type: 'info'
    });
});
```

## Delivery Topics

### 1. `/topic/deliveries/{orderId}` - Full Delivery Updates

**Description**: Receive complete delivery data updates including driver info, status, and location.

**Message Types**: `DeliveryResponse`, `OrderStatusMessage`

**Subscribe Endpoint**: `/app/deliveries/{orderId}/subscribe`

**Example**:
```javascript
stompClient.subscribe('/topic/deliveries/123', (message) => {
    const deliveryUpdate = JSON.parse(message.body);
    console.log('Delivery update:', deliveryUpdate);
    
    // Update delivery tracking UI
    updateDeliveryInfo(deliveryUpdate);
});

// Optional: Send subscription confirmation
stompClient.send('/app/deliveries/123/subscribe', {}, JSON.stringify({}));
```

**Sample Payload**:
```json
{
  "id": 789,
  "orderId": 123,
  "driverName": "John Doe",
  "driverPhone": "+1234567890",
  "vehicleInfo": "Red Honda Civic - ABC123",
  "status": "ON_THE_WAY",
  "currentLocation": "Main St & 5th Ave",
  "estimatedArrivalTime": "2025-01-16T11:00:00",
  "pickupTime": "2025-01-16T10:40:00",
  "createdAt": "2025-01-16T10:35:00",
  "updatedAt": "2025-01-16T10:45:00"
}
```

### 2. `/topic/deliveries/{orderId}/status` - Delivery Status Messages

**Description**: Receive detailed delivery status change messages.

**Message Type**: `OrderStatusMessage`

**Example**:
```javascript
stompClient.subscribe('/topic/deliveries/123/status', (message) => {
    const statusUpdate = JSON.parse(message.body);
    
    // Update delivery status badge
    updateStatusBadge(statusUpdate.status);
    
    // Show status change notification
    console.log(`Delivery ${statusUpdate.title}: ${statusUpdate.message}`);
});
```

**Sample Payload**:
```json
{
  "orderId": 123,
  "eventType": "DELIVERY_STATUS_CHANGED",
  "status": "ON_THE_WAY",
  "title": "On the Way",
  "message": "Your order is on the way",
  "timestamp": "2025-01-16T10:45:00"
}
```

### 3. `/topic/deliveries/{orderId}/location` - Real-time Location Updates

**Description**: Receive real-time GPS location updates from the delivery driver.

**Message Type**: `OrderStatusMessage` with location data

**Example**:
```javascript
stompClient.subscribe('/topic/deliveries/123/location', (message) => {
    const locationUpdate = JSON.parse(message.body);
    
    // Update map marker position
    updateDriverLocation(locationUpdate.status); // status contains location
    
    console.log('Driver location:', locationUpdate.status);
});
```

**Sample Payload**:
```json
{
  "orderId": 123,
  "eventType": "LOCATION_UPDATE",
  "status": "Main St & 5th Ave",
  "title": "Location Updated",
  "message": "Delivery location: Main St & 5th Ave",
  "timestamp": "2025-01-16T10:47:00"
}
```

### 4. `/topic/deliveries/{orderId}/notifications` - Delivery Notifications

**Description**: Receive important delivery notifications (driver assigned, arriving soon, delivered).

**Message Type**: `OrderStatusMessage`

**Example**:
```javascript
stompClient.subscribe('/topic/deliveries/123/notifications', (message) => {
    const notification = JSON.parse(message.body);
    
    // Show push notification
    if (notification.eventType === 'DRIVER_ASSIGNED') {
        showNotification({
            title: notification.title,
            body: notification.message,
            icon: 'driver-icon.png'
        });
    }
});
```

**Sample Payloads**:

**Driver Assigned**:
```json
{
  "orderId": 123,
  "eventType": "DRIVER_ASSIGNED",
  "status": "ASSIGNED",
  "title": "Driver Assigned",
  "message": "Your driver John Doe will deliver your order",
  "timestamp": "2025-01-16T10:35:00"
}
```

**Order Picked Up**:
```json
{
  "orderId": 123,
  "eventType": "ORDER_PICKED_UP",
  "status": "PICKED_UP",
  "title": "Order Picked Up",
  "message": "Your order is with the driver and on its way to you",
  "timestamp": "2025-01-16T10:40:00"
}
```

**Driver On The Way**:
```json
{
  "orderId": 123,
  "eventType": "DRIVER_ON_THE_WAY",
  "status": "ON_THE_WAY",
  "title": "Driver On The Way",
  "message": "Your order will arrive around 2025-01-16T11:00:00",
  "timestamp": "2025-01-16T10:45:00"
}
```

**Order Delivered**:
```json
{
  "orderId": 123,
  "eventType": "ORDER_DELIVERED",
  "status": "DELIVERED",
  "title": "Order Delivered",
  "message": "Your order has been successfully delivered. Enjoy your meal!",
  "timestamp": "2025-01-16T11:00:00"
}
```

## Complete Example: Order Tracking Page

```html
<!DOCTYPE html>
<html>
<head>
    <title>Order Tracking</title>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
</head>
<body>
    <div id="order-status"></div>
    <div id="delivery-status"></div>
    <div id="driver-location"></div>
    <div id="notifications"></div>

    <script>
        const orderId = 123; // Replace with actual order ID
        
        // Connect to WebSocket
        const socket = new SockJS('http://localhost:8080/ws');
        const stompClient = Stomp.over(socket);
        
        stompClient.connect({}, () => {
            console.log('Connected to WebSocket');
            
            // Subscribe to all order topics
            stompClient.subscribe(`/topic/orders/${orderId}`, (message) => {
                const order = JSON.parse(message.body);
                document.getElementById('order-status').innerHTML = 
                    `<h3>Order Status: ${order.status}</h3>
                     <p>Total: $${order.totalPrice}</p>
                     <p>Items: ${order.items.length}</p>`;
            });
            
            stompClient.subscribe(`/topic/orders/${orderId}/notifications`, (message) => {
                const notification = JSON.parse(message.body);
                addNotification(notification.title, notification.message);
            });
            
            // Subscribe to all delivery topics
            stompClient.subscribe(`/topic/deliveries/${orderId}`, (message) => {
                const delivery = JSON.parse(message.body);
                document.getElementById('delivery-status').innerHTML = 
                    `<h3>Delivery Status: ${delivery.status}</h3>
                     <p>Driver: ${delivery.driverName}</p>
                     <p>Phone: ${delivery.driverPhone}</p>
                     <p>Vehicle: ${delivery.vehicleInfo}</p>
                     <p>Location: ${delivery.currentLocation}</p>`;
            });
            
            stompClient.subscribe(`/topic/deliveries/${orderId}/location`, (message) => {
                const location = JSON.parse(message.body);
                document.getElementById('driver-location').innerHTML = 
                    `<p>📍 ${location.status}</p>`;
            });
            
            stompClient.subscribe(`/topic/deliveries/${orderId}/notifications`, (message) => {
                const notification = JSON.parse(message.body);
                addNotification(notification.title, notification.message);
            });
            
            // Send subscription confirmations
            stompClient.send(`/app/orders/${orderId}/subscribe`, {}, JSON.stringify({}));
            stompClient.send(`/app/deliveries/${orderId}/subscribe`, {}, JSON.stringify({}));
        });
        
        stompClient.onError = (error) => {
            console.error('WebSocket error:', error);
        };
        
        function addNotification(title, message) {
            const notification = document.createElement('div');
            notification.className = 'notification';
            notification.innerHTML = `<strong>${title}</strong><br>${message}`;
            document.getElementById('notifications').appendChild(notification);
        }
    </script>
</body>
</html>
```

## Internal API Endpoints (Admin Backend)

These endpoints are used by the Admin Backend to trigger WebSocket broadcasts.

### Order Status Updates

**Endpoint**: `POST /api/internal/orders/{orderId}/status`

**Request Body**:
```json
{
  "status": "PREPARING",
  "estimatedDeliveryTime": "2025-01-16T11:00:00"
}
```

**Triggers**:
- Broadcasts to `/topic/orders/{orderId}`
- Broadcasts to `/topic/orders/{orderId}/status`
- Broadcasts to `/topic/orders/{orderId}/notifications`

### Delivery Status Updates

**Endpoint**: `POST /api/internal/deliveries/{orderId}/status`

**Parameters**:
- `status` (required): `PENDING`, `ASSIGNED`, `PICKED_UP`, `ON_THE_WAY`, `DELIVERED`, `CANCELLED`
- `location` (optional): Current location string

**Example**:
```bash
curl -X POST "http://localhost:8080/api/internal/deliveries/123/status?status=ON_THE_WAY&location=Main%20St%20%26%205th%20Ave"
```

**Triggers**:
- Broadcasts to `/topic/deliveries/{orderId}`
- Broadcasts to `/topic/deliveries/{orderId}/status`
- Conditionally broadcasts to `/topic/deliveries/{orderId}/notifications`

### Delivery Location Updates

**Endpoint**: `POST /api/internal/deliveries/{orderId}/location`

**Parameters**:
- `location` (required): Current location string

**Example**:
```bash
curl -X POST "http://localhost:8080/api/internal/deliveries/123/location?location=Main%20St%20%26%206th%20Ave"
```

**Triggers**:
- Broadcasts to `/topic/deliveries/{orderId}/location`

### Assign Driver to Delivery

**Endpoint**: `POST /api/internal/deliveries/{orderId}/driver`

**Parameters**:
- `driverName` (required): Driver's name
- `driverPhone` (required): Driver's phone number
- `vehicleInfo` (optional): Vehicle information (e.g., "Red Honda Civic - ABC123")

**Example**:
```bash
curl -X POST "http://localhost:8080/api/internal/deliveries/123/driver?driverName=John%20Doe&driverPhone=%2B1234567890&vehicleInfo=Red%20Honda%20Civic%20-%20ABC123"
```

**Triggers**:
- Broadcasts to `/topic/deliveries/{orderId}`
- Broadcasts to `/topic/deliveries/{orderId}/notifications` (DRIVER_ASSIGNED event)

## Delivery Status Flow

1. **PENDING** → Order placed, waiting for driver assignment
2. **ASSIGNED** → Driver assigned to delivery
3. **PICKED_UP** → Driver picked up the order from restaurant
4. **ON_THE_WAY** → Driver is on the way to customer
5. **DELIVERED** → Order successfully delivered
6. **CANCELLED** → Delivery cancelled

## Event Types

### Order Events
- `SUBSCRIPTION_CONFIRMED` - Client successfully subscribed
- `ORDER_STATUS_CHANGED` - Order status changed

### Delivery Events
- `SUBSCRIPTION_CONFIRMED` - Client successfully subscribed
- `DELIVERY_STATUS_CHANGED` - Delivery status changed
- `LOCATION_UPDATE` - Driver location updated
- `DRIVER_ASSIGNED` - Driver assigned to delivery
- `ORDER_PICKED_UP` - Order picked up by driver
- `DRIVER_ON_THE_WAY` - Driver is on the way
- `ORDER_DELIVERED` - Order delivered successfully

## Best Practices

1. **Always handle errors**: Implement proper error handling for WebSocket connections
2. **Reconnect logic**: Implement automatic reconnection on disconnect
3. **Unsubscribe when done**: Clean up subscriptions when leaving the page
4. **Use subscription confirmations**: Send subscription messages to verify connection
5. **Handle all message types**: Be prepared to handle both full objects and status messages
6. **Show loading states**: Display loading indicators while connecting
7. **Fallback to polling**: Have a fallback mechanism if WebSocket fails

## Troubleshooting

### Connection Issues

**Problem**: Cannot connect to WebSocket endpoint

**Solutions**:
- Verify the endpoint URL is correct
- Check CORS configuration in `WebSocketConfig.java`
- Ensure SockJS and STOMP libraries are loaded
- Check browser console for errors

### Not Receiving Messages

**Problem**: Connected but not receiving updates

**Solutions**:
- Verify you're subscribed to the correct topic
- Check that the orderId is correct
- Ensure order/delivery exists in database
- Check server logs for broadcast messages

### Messages Not Broadcasting

**Problem**: Server not broadcasting WebSocket messages

**Solutions**:
- Verify `DeliveryService` methods are being called
- Check server logs for WebSocket controller activity
- Ensure `@Transactional` is working properly
- Verify `SimpMessagingTemplate` bean is available

## REST API Documentation

For complete REST API documentation including WebSocket info:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **WebSocket Info**: GET /api/websocket/info

## Related Documentation

- [WebSocket Documentation](WEBSOCKET_DOCUMENTATION.md)
- [WebSocket Upgrade Guide](WEBSOCKET_UPGRADE_GUIDE.md)
- [API Documentation](API_DOCUMENTATION.md)
- [Testing Guide](TESTING_GUIDE.md)
