# Driver Location Tracking Implementation

## Summary
This implementation adds real-time driver location tracking with Leaflet maps to the restaurant store API.

## Changes Made

### 1. Database Schema Updates

#### Order Entity (`Order.java`)
- Added `unique = true` constraint to `external_id` column to prevent duplicates
- This fixes the `IncorrectResultSizeDataAccessException` error when polling orders

#### Delivery Entity (`Delivery.java`)
- Added `latitude` field (Double) - Driver's current latitude
- Added `longitude` field (Double) - Driver's current longitude

### 2. DTOs Updated

#### DeliveryDTO (`dto/admin/DeliveryDTO.java`)
- Added `latitude` field
- Added `longitude` field
- These fields match the admin-api.json schema

#### DeliveryResponse (`dto/response/DeliveryResponse.java`)
- Added `latitude` field
- Added `longitude` field

### 3. Service Layer

#### DeliveryService
- Added overloaded `updateDeliveryLocation(Long orderId, Double latitude, Double longitude)` method
- This method updates both lat/lng fields and broadcasts via WebSocket

#### DeliveryMapper
- Updated `toResponse()` to include latitude and longitude fields

### 4. API Layer

#### DeliveryController
- Updated `/api/deliveries/{orderId}/location` endpoint to accept both:
  - `location` (String) - legacy support
  - `latitude` and `longitude` (Double) - new coordinate-based updates
- Endpoint now handles both formats and calls appropriate service method

### 5. Error Handling

#### AdminOrderEventForwarder
- Added try-catch blocks around `findByExternalId()` calls
- Falls back to `findById()` if duplicate external_id error occurs
- Logs warnings for debugging but doesn't fail the entire sync process

### 6. Frontend - Leaflet Map Integration

#### order-details.html
**Added Dependencies:**
- Leaflet CSS and JS from CDN (version 1.9.4)

**Added Map Styles:**
- `#deliveryMap` - 400px height, rounded corners, border

**Added Map Container:**
- New card section showing "Driver Location" map
- Only visible for delivery orders with location data

**JavaScript Enhancements:**
- Added map instance variables: `deliveryMap`, `driverMarker`, `destinationMarker`
- Added `hasDriverLocation()` - checks if lat/lng exist
- Added `initializeMap()` - creates Leaflet map with driver marker
- Added `updateDriverLocation(lat, lng)` - updates marker position in real-time
- Updated `applyOrderUpdate()` - triggers map update when location changes
- Updated `extractMetadataUpdates()` - extracts latitude/longitude from WebSocket messages
- Updated `teardown()` - properly cleans up map instance

**Custom Driver Marker:**
- Blue circular marker with car icon
- Drop shadow and white border
- Popup showing "Driver Location"

### 7. WebSocket Integration

The existing WebSocket infrastructure automatically handles location updates:
- Admin/Driver can POST to `/api/deliveries/{orderId}/location` with lat/lng
- DeliveryService broadcasts update via WebSocket
- Frontend receives update and moves marker in real-time

## How to Use

### For Admin Backend (Updating Driver Location)

```bash
# Update driver location with coordinates
curl -X POST "http://localhost:8080/api/deliveries/{orderId}/location?latitude=11.5564&longitude=104.9282"

# Or with legacy string format
curl -X POST "http://localhost:8080/api/deliveries/{orderId}/location?location=11.5564,104.9282"
```

### For Customer Frontend (Viewing Driver Location)

1. Navigate to order details page: `/orders/{orderId}`
2. If the order is a delivery and has driver location data, a map will appear
3. The map shows the driver's current location with a blue car marker
4. As the driver moves, the marker updates in real-time via WebSocket

## Database Migration Notes

### Cleaning Up Duplicate external_id Values

If you encounter the duplicate external_id error in existing data, run this SQL:

```sql
-- Find duplicates
SELECT external_id, COUNT(*) 
FROM orders 
WHERE external_id IS NOT NULL 
GROUP BY external_id 
HAVING COUNT(*) > 1;

-- Option 1: Set duplicates to NULL (keeps one)
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

-- Option 2: Delete duplicate orders (if they're test data)
WITH ranked AS (
    SELECT id, 
           ROW_NUMBER() OVER (PARTITION BY external_id ORDER BY created_at) as rn
    FROM orders
    WHERE external_id IS NOT NULL
)
DELETE FROM orders
WHERE id IN (
    SELECT id FROM ranked WHERE rn > 1
);
```

After cleaning duplicates, restart the application. JPA will apply the unique constraint.

## Testing

### Test Endpoints

1. **Update Location:**
   ```bash
   curl -X POST "http://localhost:8080/api/deliveries/506/location?latitude=11.5564&longitude=104.9282"
   ```

2. **View Delivery:**
   ```bash
   curl -X GET "http://localhost:8080/api/deliveries/506" \
        -H "Authorization: Bearer YOUR_TOKEN"
   ```

3. **Check WebSocket:**
   - Open browser console
   - Navigate to order details page
   - Watch for WebSocket messages with location updates

### Expected Behavior

1. Driver updates location via API
2. DeliveryService saves lat/lng to database
3. DeliveryService broadcasts update via WebSocket to `/topic/deliveries/{orderId}/location`
4. Frontend receives WebSocket message
5. Map marker smoothly moves to new position

## Architecture Decisions

### Why Leaflet?
- Lightweight (39KB gzipped)
- Works with any tile provider (OpenStreetMap)
- No API key required for basic usage
- Mobile-friendly
- Easy to customize markers

### Why Store Coordinates in Delivery Entity?
- Delivery-specific data (not all orders have deliveries)
- Allows historical tracking
- Can be used for analytics (distance calculations, route optimization)

### Why Both String and Coordinate Fields?
- `currentLocation` (String) - legacy support, human-readable
- `latitude`/`longitude` (Double) - precise, map-ready
- The String field is auto-populated from coordinates when both are provided

## Future Enhancements

1. **Route Polyline**: Draw line from restaurant to destination
2. **ETA Calculation**: Calculate estimated arrival based on distance
3. **Multiple Markers**: Show restaurant location and destination
4. **Live Tracking**: More frequent location updates (every 10 seconds)
5. **Geofencing**: Trigger notifications when driver enters delivery zone
6. **Historical Route**: Show path driver has taken
7. **Traffic Layer**: Overlay real-time traffic data
8. **Driver Photo**: Show driver's photo on marker popup

## Related Files

- `src/main/java/com/restaurant/store/entity/Order.java`
- `src/main/java/com/restaurant/store/entity/Delivery.java`
- `src/main/java/com/restaurant/store/dto/admin/DeliveryDTO.java`
- `src/main/java/com/restaurant/store/dto/response/DeliveryResponse.java`
- `src/main/java/com/restaurant/store/service/DeliveryService.java`
- `src/main/java/com/restaurant/store/controller/api/DeliveryController.java`
- `src/main/java/com/restaurant/store/mapper/DeliveryMapper.java`
- `src/main/java/com/restaurant/store/integration/AdminOrderEventForwarder.java`
- `src/main/resources/templates/order-details.html`

## Admin API Compliance

This implementation now fully complies with the `admin-api.json` schema:

### DeliveryDTO Schema Match
```json
{
  "latitude": {
    "type": "number",
    "format": "double"
  },
  "longitude": {
    "type": "number",
    "format": "double"
  }
}
```

### OrderDTO Schema
- All order statuses match: PENDING, CONFIRMED, PREPARING, READY_FOR_PICKUP, READY_FOR_DELIVERY, OUT_FOR_DELIVERY, COMPLETED, CANCELLED

### Location Update Description
The admin API documentation includes:
> "Supports optional geolocation data (latitude/longitude) for delivery orders to enable location-based features."

This is now fully implemented and working.
