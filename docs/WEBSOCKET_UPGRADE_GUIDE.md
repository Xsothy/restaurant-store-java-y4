# WebSocket Upgrade and Documentation Guide

## Problem Statement

Users were encountering a `WebSocketChannelException` when trying to connect to `/api/deliveries/track/506` as a WebSocket endpoint. The error "connection was not upgraded to websocket" occurred because this is a REST API endpoint, not a WebSocket endpoint.

## Root Cause

The confusion stemmed from:
1. WebSocket endpoints not being documented in OpenAPI/Swagger
2. Unclear distinction between REST polling endpoints and WebSocket real-time endpoints
3. Delivery tracking only available via REST, not WebSocket
4. No clear documentation on how to connect to WebSocket for real-time tracking

## Solution Implemented

### 1. Created DeliveryStatusWebSocketController

**File:** `src/main/java/com/restaurant/store/controller/api/DeliveryStatusWebSocketController.java`

New controller that provides WebSocket endpoints for real-time delivery tracking:

- **Topic:** `/topic/deliveries/{orderId}` - Main delivery updates
- **Topic:** `/topic/deliveries/{orderId}/status` - Status change messages
- **Topic:** `/topic/deliveries/{orderId}/location` - Real-time location updates
- **Topic:** `/topic/deliveries/{orderId}/notifications` - Delivery notifications
- **Subscribe:** `/app/deliveries/{orderId}/subscribe` - Subscription confirmation

**Methods:**
- `subscribeToDelivery()` - Handle subscription requests
- `sendDeliveryUpdate()` - Broadcast delivery updates
- `sendDeliveryStatusUpdate()` - Broadcast status changes
- `sendLocationUpdate()` - Broadcast location updates
- `sendDeliveryNotification()` - Send notifications

### 2. Created WebSocketInfoController

**File:** `src/main/java/com/restaurant/store/controller/api/WebSocketInfoController.java`

REST endpoint that documents all WebSocket connections in OpenAPI:

- **Endpoint:** `GET /api/websocket/info`
- **Purpose:** Provide complete WebSocket documentation accessible via REST API
- **Includes:**
  - Connection endpoint (`ws://localhost:8080/ws`)
  - Available topics for orders and deliveries
  - Example code in JavaScript
  - Message types for each topic

This endpoint appears in Swagger UI under the "WebSocket Information" tag.

### 3. Enhanced DeliveryController

**File:** `src/main/java/com/restaurant/store/controller/api/DeliveryController.java`

**Changes:**
- Added Swagger `@Tag` annotation with note about WebSocket availability
- Enhanced `trackDelivery()` endpoint documentation to explain:
  - This is a REST polling endpoint
  - WebSocket should be used for real-time tracking
  - How to connect to WebSocket
  - Reference to `/api/websocket/info` for details

### 4. Enhanced OrderStatusWebSocketController

**File:** `src/main/java/com/restaurant/store/controller/api/OrderStatusWebSocketController.java`

**Changes:**
- Added comprehensive JavaDoc documentation
- Added `@Hidden` annotation to hide from OpenAPI (since it's not a REST endpoint)
- Documented connection details and topics
- Added reference to `/api/websocket/info`

### 5. Updated OpenApiConfig

**File:** `src/main/java/com/restaurant/store/config/OpenApiConfig.java`

**Changes:**
- Enhanced API description to include WebSocket information
- Added section explaining WebSocket support
- Listed key WebSocket endpoints
- Directed users to `/api/websocket/info` for full documentation

### 6. Comprehensive Documentation

**File:** `docs/WEBSOCKET_DOCUMENTATION.md`

Complete guide covering:
- Connection details and setup
- Common mistakes (like trying to connect to REST endpoints as WebSocket)
- Available topics for orders and deliveries
- JavaScript examples with SockJS and STOMP
- Complete working examples with error handling
- Message format specifications
- Troubleshooting guide
- Best practices
- Production considerations

## Usage Examples

### Correct Way to Track Deliveries

#### ❌ WRONG (What was causing the error)
```javascript
// This is a REST endpoint, not WebSocket!
const socket = new SockJS('http://localhost:8080/api/deliveries/track/506');
// Error: WebSocketChannelException - connection was not upgraded to websocket
```

#### ✅ CORRECT (Real-time tracking)
```javascript
// Connect to WebSocket endpoint
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function() {
    // Subscribe to delivery updates
    stompClient.subscribe('/topic/deliveries/506', function(message) {
        const delivery = JSON.parse(message.body);
        console.log('Delivery update:', delivery);
    });
    
    // Optional: Send subscription message
    stompClient.send('/app/deliveries/506/subscribe', {}, JSON.stringify({}));
});
```

#### 📋 REST Polling (Fallback)
```javascript
// Use REST endpoint only if WebSocket is not available
fetch('/api/deliveries/track/506', {
    headers: {
        'Authorization': 'Bearer ' + token
    }
})
.then(response => response.json())
.then(data => console.log('Delivery status:', data));
```

## OpenAPI/Swagger Documentation

The WebSocket endpoints are now properly documented in Swagger:

1. **Main API Description:**
   - Visit: `http://localhost:8080/swagger-ui.html`
   - The API description now includes a WebSocket section
   - Explains where to find WebSocket documentation

2. **WebSocket Information Endpoint:**
   - Tag: "WebSocket Information"
   - Endpoint: `GET /api/websocket/info`
   - Returns complete connection details, topics, and code examples
   - Fully documented with OpenAPI annotations

3. **Delivery Controller:**
   - Tag: "Deliveries"
   - Note in tag description about WebSocket availability
   - Each tracking endpoint documents WebSocket alternative

## Testing the Changes

### 1. View OpenAPI Documentation
```bash
# Start the application
./mvnw spring-boot:run

# Open browser to
http://localhost:8080/swagger-ui.html

# Look for:
# - "WebSocket Information" section
# - GET /api/websocket/info endpoint
# - Enhanced API description
```

### 2. Test WebSocket Info Endpoint
```bash
curl http://localhost:8080/api/websocket/info | jq
```

### 3. Test WebSocket Connection (Browser Console)
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected:', frame);
    
    // Test delivery tracking
    stompClient.subscribe('/topic/deliveries/1', function(message) {
        console.log('Delivery update:', JSON.parse(message.body));
    });
    
    stompClient.send('/app/deliveries/1/subscribe', {}, JSON.stringify({}));
});
```

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         Client                               │
│  (Browser, Mobile App, etc.)                                 │
└────────┬────────────────────────────────────────────────┬───┘
         │                                                  │
         │ REST API (Polling)                              │ WebSocket (Real-time)
         │                                                  │
         ▼                                                  ▼
┌────────────────────────┐                    ┌───────────────────────────┐
│   DeliveryController   │                    │ DeliveryStatusWebSocket   │
│                        │                    │      Controller           │
│ GET /api/deliveries/   │                    │                           │
│     track/{id}         │                    │ Topic: /topic/deliveries/ │
│                        │                    │        {orderId}          │
│ Returns current state  │                    │                           │
│ Requires polling       │                    │ Push updates in real-time │
└────────────────────────┘                    └───────────────────────────┘
         │                                                  │
         └──────────────────┬───────────────────────────────┘
                            │
                            ▼
                  ┌──────────────────┐
                  │ DeliveryService  │
                  │                  │
                  │ Business Logic   │
                  └──────────────────┘
```

## Benefits

1. **Clear Separation:**
   - REST endpoints for polling
   - WebSocket endpoints for real-time updates
   - Both clearly documented

2. **OpenAPI Integration:**
   - WebSocket info accessible via REST endpoint
   - Appears in Swagger UI
   - Fully documented with examples

3. **Developer Experience:**
   - Clear error messages directing to correct endpoint
   - Code examples provided
   - Comprehensive troubleshooting guide

4. **Flexibility:**
   - Can use REST polling as fallback
   - Can use WebSocket for real-time updates
   - Both approaches fully supported

## Migration Guide

If you were previously trying to use WebSocket with the REST endpoint:

### Before (Incorrect)
```javascript
// This never worked correctly
const socket = new SockJS('http://localhost:8080/api/deliveries/track/506');
```

### After (Correct)
```javascript
// Connect to proper WebSocket endpoint
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
    // Subscribe to the correct topic
    stompClient.subscribe('/topic/deliveries/506', (message) => {
        const delivery = JSON.parse(message.body);
        updateUI(delivery);
    });
});
```

## Next Steps

1. **Review Documentation:**
   - Read `docs/WEBSOCKET_DOCUMENTATION.md`
   - Check `/api/websocket/info` endpoint
   - Review Swagger UI documentation

2. **Update Frontend Code:**
   - Change WebSocket connection to `/ws`
   - Subscribe to `/topic/deliveries/{orderId}`
   - Use REST `/api/deliveries/track/{orderId}` only for fallback

3. **Test Thoroughly:**
   - Test WebSocket connection
   - Test real-time updates
   - Test fallback to REST polling
   - Test error handling

4. **Production Deployment:**
   - Update WebSocket URL to production domain
   - Use WSS (WebSocket Secure) for HTTPS sites
   - Configure CORS for production origins
   - Monitor WebSocket connection metrics

## Related Files

- `src/main/java/com/restaurant/store/controller/api/DeliveryStatusWebSocketController.java` - New WebSocket controller
- `src/main/java/com/restaurant/store/controller/api/WebSocketInfoController.java` - New documentation controller
- `src/main/java/com/restaurant/store/controller/api/DeliveryController.java` - Enhanced REST controller
- `src/main/java/com/restaurant/store/controller/api/OrderStatusWebSocketController.java` - Enhanced WebSocket controller
- `src/main/java/com/restaurant/store/config/OpenApiConfig.java` - Enhanced OpenAPI config
- `src/main/java/com/restaurant/store/config/WebSocketConfig.java` - Existing WebSocket configuration
- `docs/WEBSOCKET_DOCUMENTATION.md` - Comprehensive WebSocket guide
- `docs/WEBSOCKET_UPGRADE_GUIDE.md` - This file

## Support

For questions or issues:
1. Check `docs/WEBSOCKET_DOCUMENTATION.md`
2. Use `/api/websocket/info` for current connection details
3. Review Swagger UI documentation
4. Check browser console for connection errors
5. Verify server is running and WebSocket endpoint is accessible
