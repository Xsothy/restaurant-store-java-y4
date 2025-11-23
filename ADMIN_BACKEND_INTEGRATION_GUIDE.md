# Admin Backend Integration Guide

## Overview
This guide explains how the Admin Backend should integrate with the Store API to sync delivery information, including real-time driver location tracking.

## Delivery Location Tracking

### Updating Driver Location

The Store API provides endpoints for the Admin Backend to update driver location in real-time.

#### Endpoint
```
POST /api/deliveries/{orderId}/location
```

#### Request Parameters

**Option 1: Using Coordinates (Recommended)**
```bash
curl -X POST "http://localhost:8080/api/deliveries/{orderId}/location" \
  -d "latitude=11.5564" \
  -d "longitude=104.9282"
```

**Option 2: Using String Location (Legacy)**
```bash
curl -X POST "http://localhost:8080/api/deliveries/{orderId}/location" \
  -d "location=11.5564,104.9282"
```

#### Response
```json
{
  "success": true,
  "message": "Delivery location updated successfully",
  "data": "Location updated to: 11.556400,104.928200"
}
```

### Automatic Sync from Admin API

When the Store API polls orders from the Admin Backend (GET `/api/orders`), it automatically extracts delivery information including location coordinates.

#### Expected Response Format from Admin API

```json
{
  "success": true,
  "message": "Orders retrieved successfully",
  "data": {
    "content": [
      {
        "id": 2,
        "status": "COMPLETED",
        "totalPrice": 12000,
        "orderType": "DELIVERY",
        "delivery": {
          "id": 2,
          "orderId": 2,
          "driver": {
            "id": 4,
            "username": "driver1",
            "fullName": "John Delivery",
            "email": "driver1@resadmin.com"
          },
          "status": "OUT_FOR_DELIVERY",
          "latitude": 11.5564,
          "longitude": 104.9282,
          "deliveryAddress": "123 Street, Phnom Penh",
          "deliveryNotes": "Call on arrival",
          "dispatchedAt": "2025-11-23T19:43:15.320616",
          "deliveredAt": null
        }
      }
    ]
  }
}
```

#### What Gets Synced

The Store API automatically syncs the following fields from the Admin API:

**Delivery Entity Fields:**
- `latitude` - Driver's current latitude
- `longitude` - Driver's current longitude
- `status` - Delivery status (PENDING, ASSIGNED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED)
- `deliveryAddress` - Delivery destination address
- `deliveryNotes` - Special delivery instructions
- `driver.fullName` → `driverName` - Driver's full name
- `driver.email` → `driverPhone` - Driver's contact (email used as fallback)
- `dispatchedAt` → `estimatedDeliveryTime` - When delivery was dispatched
- `deliveredAt` → `actualDeliveryTime` - When order was delivered

### Real-Time Updates

When location is updated via the Store API, it broadcasts the update via WebSocket to all subscribed clients.

#### WebSocket Topics
- `/topic/deliveries/{orderId}` - Full delivery updates
- `/topic/deliveries/{orderId}/location` - Location-only updates

#### WebSocket Message Format
```json
{
  "latitude": 11.5564,
  "longitude": 104.9282
}
```

## Delivery Status Mapping

The Store API uses a different delivery status enum than the Admin Backend. Here's the mapping:

### Admin Backend Statuses
- `PENDING` - Waiting for driver assignment
- `ASSIGNED` - Driver assigned
- `OUT_FOR_DELIVERY` - Driver is delivering
- `DELIVERED` - Order delivered
- `CANCELLED` - Delivery cancelled

### Store API Statuses
- `PENDING` - Waiting for driver assignment
- `ASSIGNED` - Driver assigned
- `PICKED_UP` - Driver picked up from restaurant
- `ON_THE_WAY` - Driver is delivering
- `DELIVERED` - Order delivered
- `CANCELLED` - Delivery cancelled

**Note:** The Store API maps `OUT_FOR_DELIVERY` from Admin Backend to `ON_THE_WAY` internally.

## Order Status Synchronization

### Supported Order Statuses

Both systems support the following order statuses:
- `PENDING` - Order received
- `CONFIRMED` - Restaurant confirmed
- `PREPARING` - Being prepared
- `READY_FOR_PICKUP` - Ready for pickup
- `READY_FOR_DELIVERY` - Ready for delivery
- `OUT_FOR_DELIVERY` - Being delivered
- `COMPLETED` - Completed
- `CANCELLED` - Cancelled

### Status Update Flow

1. **Store API → Admin Backend**: When order status changes in Store API, it syncs to Admin via:
   ```
   PATCH /api/orders/{id}/status
   ```

2. **Admin Backend → Store API**: When order status changes in Admin, Store API polls via:
   ```
   GET /api/orders/kitchen
   GET /api/orders/delivery
   ```

## Polling Configuration

The Store API polls the Admin Backend at regular intervals to sync order and delivery data.

### Configuration Properties
```properties
# Enable/disable polling
admin.api.order-status.polling.enabled=true

# Polling interval in milliseconds (default: 2000ms = 2 seconds)
admin.api.order-status.polling.interval=2000

# Admin API credentials
admin.api.url=http://localhost:8081
admin.api.username=admin
admin.api.password=password123
```

### Polling Endpoints
- `GET /api/orders/kitchen` - Polls kitchen orders
- `GET /api/orders/delivery` - Polls delivery orders (includes location data)

## External ID Management

### Purpose
The `external_id` field links Store API orders to Admin Backend orders.

### Constraint
**Important:** The `external_id` field has a UNIQUE constraint. This prevents duplicate order syncing.

### Handling Duplicates

If you encounter "Query did not return a unique result" errors:

```sql
-- Find duplicates
SELECT external_id, COUNT(*) 
FROM orders 
WHERE external_id IS NOT NULL 
GROUP BY external_id 
HAVING COUNT(*) > 1;

-- Fix duplicates (keeps first occurrence)
WITH ranked AS (
    SELECT id, external_id, 
           ROW_NUMBER() OVER (PARTITION BY external_id ORDER BY created_at) as rn
    FROM orders
    WHERE external_id IS NOT NULL
)
UPDATE orders
SET external_id = NULL
WHERE id IN (
    SELECT id FROM ranked WHERE rn > 1
);
```

## Driver App Integration

### Mobile Driver Location Updates

The Admin Backend's driver mobile app should update location periodically:

```javascript
// Pseudo-code for driver app
setInterval(async () => {
  const position = await getCurrentPosition();
  
  // Send to Admin Backend
  await fetch(`${ADMIN_API}/api/deliveries/${orderId}/location`, {
    method: 'POST',
    body: JSON.stringify({
      latitude: position.coords.latitude,
      longitude: position.coords.longitude
    })
  });
  
  // Admin Backend will sync to Store API
}, 10000); // Update every 10 seconds
```

### Update Frequency Recommendations

- **Active Delivery**: Every 5-10 seconds
- **Idle/Waiting**: Every 30-60 seconds
- **Battery Optimization**: Use geofencing to reduce updates when stationary

## Customer Frontend Display

### Leaflet Map Integration

The Store API's customer-facing web interface displays driver location using Leaflet maps.

#### Map Features
- Real-time driver location marker
- Custom blue car icon
- Auto-updates via WebSocket
- Smooth marker transitions

#### Viewing the Map

Customers can view driver location at:
```
http://localhost:8080/orders/{orderId}
```

Requirements:
- Order must be a DELIVERY order
- Delivery must have latitude and longitude coordinates
- Customer must be authenticated

### Map Screenshot Example

When latitude and longitude are provided, customers see:
- A Leaflet map with OpenStreetMap tiles
- A blue circular marker with a car icon
- A popup showing "Driver Location"
- Real-time position updates as driver moves

## Testing

### Test Script

Use the provided test script:

```bash
chmod +x test-driver-location.sh
./test-driver-location.sh
```

### Manual Testing

1. **Create an order in Store API**
   ```bash
   # Login and place order via Store API
   ```

2. **Update driver location via Admin Backend**
   ```bash
   curl -X POST "http://localhost:8080/api/deliveries/2/location" \
     -d "latitude=11.5564" \
     -d "longitude=104.9282"
   ```

3. **Verify on customer frontend**
   - Login as customer
   - Go to order details page
   - See map with driver location

### WebSocket Testing

Use browser console to monitor WebSocket messages:

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  stompClient.subscribe('/topic/deliveries/2/location', (message) => {
    console.log('Location update:', JSON.parse(message.body));
  });
});
```

## API Endpoints Summary

### Store API Endpoints (for Admin Backend)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/deliveries/{orderId}/location` | Update driver location |
| POST | `/api/deliveries/{orderId}/status` | Update delivery status |
| POST | `/api/deliveries/{orderId}/driver` | Assign driver |
| GET | `/api/websocket/info` | WebSocket documentation |

### Admin Backend Endpoints (polled by Store API)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/orders/kitchen` | Get kitchen orders |
| GET | `/api/orders/delivery` | Get delivery orders |
| GET | `/api/orders` | Get all orders |
| GET | `/api/deliveries` | Get all deliveries |
| PATCH | `/api/orders/{id}/status` | Update order status |

## Error Handling

### Common Issues

#### 1. Duplicate external_id
**Error:** `Query did not return a unique result: 2 results were returned`

**Solution:** Clean up duplicates using SQL above, or set `external_id` to NULL for duplicates.

#### 2. Delivery not found
**Error:** `Delivery not found for order id: {orderId}`

**Solution:** Ensure delivery record exists before updating location. Check order type is DELIVERY.

#### 3. Authentication failed
**Error:** `Unable to authenticate with Admin API`

**Solution:** Verify `admin.api.username` and `admin.api.password` in application.properties.

#### 4. Map not showing
**Issue:** Map container appears but no map renders

**Solution:**
- Check browser console for errors
- Verify latitude and longitude are not null
- Ensure Leaflet CSS and JS are loaded
- Check that order type is DELIVERY

## Best Practices

### 1. Location Update Frequency
- **Recommended:** 10-30 seconds during active delivery
- **Minimum:** 5 seconds (to avoid overwhelming the server)
- **Maximum:** 60 seconds (to maintain real-time feel)

### 2. Battery Optimization
- Reduce update frequency when driver is stationary
- Use significant location change detection
- Implement geofencing around restaurant/destination

### 3. Network Optimization
- Batch multiple updates if network is slow
- Implement retry logic with exponential backoff
- Cache last known location locally

### 4. Privacy Considerations
- Only share location during active delivery
- Stop location updates when order is delivered
- Allow drivers to pause location sharing

### 5. Error Recovery
- Log all location update failures
- Retry failed updates up to 3 times
- Fall back to last known location if updates fail

## Support

For issues or questions:
1. Check logs in `logs/store-api.log`
2. Review WebSocket messages in browser console
3. Verify Admin API is returning correct data format
4. Check database for duplicate external_ids

## Related Documentation

- `LOCATION_TRACKING_IMPLEMENTATION.md` - Technical implementation details
- `docs/WEBSOCKET_DOCUMENTATION.md` - WebSocket usage guide
- `docs/API_DOCUMENTATION.md` - Full API reference
