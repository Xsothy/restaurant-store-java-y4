# Restaurant Store API - Integration Documentation

## Overview

This Restaurant Store API serves as the customer-facing backend for a Flutter mobile application. It integrates with the Restaurant Admin Backend for menu synchronization and order management, and uses Stripe for payment processing.

## Architecture

```
┌─────────────────┐         ┌──────────────────────┐         ┌─────────────┐
│  Flutter App    │ ◄─────► │ Restaurant Store API │ ◄─────► │ Admin API   │
└─────────────────┘         └──────────────────────┘         └─────────────┘
                                      │
                                      ▼
                            ┌──────────────────┐
                            │  Stripe Payment  │
                            └──────────────────┘
```

## Key Features

### 1. Admin Backend Integration

The Store API integrates with the Admin Backend to:
- **Sync Menu Data**: Categories and products are fetched from Admin API
- **Sync Orders**: Orders created by customers are synced to Admin for processing
- **Receive Status Updates**: Admin can push order status updates via internal API

#### Configuration

```properties
# application.properties
admin.api.url=http://localhost:8081/api
admin.api.username=admin
admin.api.password=admin123
admin.api.sync.enabled=true
admin.api.sync.interval=300000  # 5 minutes in milliseconds
```

#### Data Sync

- **Automatic Sync**: Runs on application startup and every 5 minutes (configurable)
- **Manual Sync**: Available via `/api/sync/*` endpoints
- **External ID Tracking**: Each synced entity stores the Admin API's ID

**Sync Endpoints:**
- `POST /api/sync/all` - Sync all data
- `POST /api/sync/categories` - Sync categories only
- `POST /api/sync/products` - Sync products only

### 2. Stripe Payment Integration

The Store API uses Stripe for secure payment processing.

#### Configuration

```properties
stripe.api.key=sk_test_your_stripe_secret_key_here
stripe.webhook.secret=whsec_your_webhook_secret_here
```

#### Payment Flow

1. **Create Payment Intent**: `POST /api/orders/{orderId}/payment-intent`
   - Returns `clientSecret` for Stripe.js/SDK
   - Creates a pending payment record

2. **Process Payment**: `POST /api/orders/{orderId}/pay`
   - For card payments: Confirms the payment intent
   - For cash payments: Marks payment as completed directly

3. **Webhook**: `POST /api/webhooks/stripe`
   - Receives payment status updates from Stripe
   - Updates payment and order status

### 3. WebSocket for Real-Time Updates

WebSocket support for pushing order status updates to Flutter app.

#### Configuration

```properties
websocket.allowed.origins=*
```

#### Usage

**Connect to WebSocket:**
```
ws://localhost:8080/ws
```

**Subscribe to Order Updates:**
```
/topic/orders/{orderId}
```

The Admin Backend can push status updates via the Internal API, which will be broadcast to connected clients.

#### Admin WebSocket Bridge

To keep the Admin backend as the single source of truth for tracking events, the Store API can connect directly to the Admin WebSocket endpoint and relay each message to customer devices. Set the following properties (enabled by default in `application.properties`). The bridge only starts when `admin.api.order-status.polling.enabled=false`:

```properties
admin.api.websocket.bridge.enabled=true
admin.api.websocket.url=ws://localhost:8081/ws
admin.api.websocket.topic=/topic/admin/orders
```

When the bridge is enabled, local WebSocket broadcasts are suppressed and only Admin-sourced updates are sent to customers, ensuring both systems see identical tracking events.

#### Admin Order Polling Mode

Set `admin.api.order-status.polling.enabled=true` to switch the store into polling mode and completely disable the Admin WebSocket bridge. This is useful when the upstream WebSocket endpoint is offline or not yet available, but you still want customers to receive frequent status updates:

```properties
admin.api.order-status.polling.enabled=true
admin.api.order-status.polling.interval=2000   # milliseconds between checks
admin.api.order-status.polling.statuses=PENDING,CONFIRMED,PREPARING,READY_FOR_PICKUP,READY_FOR_DELIVERY,OUT_FOR_DELIVERY
```

While polling mode is enabled, the Store API queries the Admin `/orders` endpoint at the configured interval and reuses the same WebSocket broadcasters to notify connected mobile and web clients about any status changes. The WebSocket bridge beans will not start until you set `admin.api.order-status.polling.enabled=false` again.

### 4. Internal API for Admin Backend

These endpoints allow the Admin Backend to update order statuses and sync information.

**Endpoints:**

- `POST /api/internal/orders/{orderId}/status`
  - Updates order status
  - Triggers WebSocket notification to customer

- `POST /api/internal/orders/{orderId}/sync?externalId={id}`
  - Links Store order with Admin order ID

## API Endpoints

### Customer Authentication

- `POST /api/auth/register` - Register new customer
- `POST /api/auth/login` - Login and receive JWT token
- `POST /api/auth/logout` - Logout

### Menu Browsing

- `GET /api/categories` - Get all categories
- `GET /api/products` - Get all products
- `GET /api/products/{id}` - Get product by ID

### Order Management

- `POST /api/orders` - Create new order
- `GET /api/orders/{id}` - Get order details
- `GET /api/orders/{id}/status` - Get order status
- `GET /api/orders/my-orders` - Get customer's orders
- `PUT /api/orders/{id}/cancel` - Cancel order

### Payment

- `POST /api/orders/{id}/payment-intent` - Create Stripe payment intent
- `POST /api/orders/{id}/pay` - Process payment

### Delivery Tracking

- `GET /api/deliveries/{orderId}` - Get delivery information

## Database Schema

### New Fields for Integration

All entities that sync with Admin Backend have:
- `external_id` - ID from Admin API
- `synced_at` - Last sync timestamp

**Entities with sync support:**
- `categories`
- `products`
- `orders`

## Responsibilities

### Store API (This Application)

✅ Customer registration and authentication  
✅ Order creation and placement  
✅ Payment processing (Stripe)  
✅ Order/delivery status retrieval  
✅ Menu data caching and serving  

### Admin Backend (External)

✅ Menu/product/category management  
✅ Order status updates (Confirmed, Preparing, Ready, etc.)  
✅ Delivery management and driver assignment  
✅ Inventory/stock management  

## Setup Instructions

### 1. Configure Admin Backend

Update `application.properties` with your Admin API details:

```properties
admin.api.url=http://your-admin-backend:8081/api
admin.api.username=your-admin-username
admin.api.password=your-admin-password
```

### 2. Configure Stripe

1. Get your Stripe API keys from [Stripe Dashboard](https://dashboard.stripe.com)
2. Update `application.properties`:

```properties
stripe.api.key=sk_test_your_actual_key
stripe.webhook.secret=whsec_your_actual_secret
```

3. Configure Stripe webhook endpoint:
   - URL: `https://your-domain.com/api/webhooks/stripe`
   - Events: `payment_intent.succeeded`, `payment_intent.payment_failed`

### 3. Run the Application

```bash
mvn clean install
mvn spring-boot:run
```

### 4. Initial Data Sync

On startup, the application will automatically sync categories and products from the Admin API.

You can also trigger manual sync:

```bash
curl -X POST http://localhost:8080/api/sync/all
```

## Development Notes

### Testing Without Admin Backend

If the Admin Backend is not available, the application will:
- Log errors but continue running
- Use any existing cached data
- Skip automatic sync

You can also disable sync:

```properties
admin.api.sync.enabled=false
```

### Testing Without Stripe

For local development without Stripe:
- Use `CASH` or other payment methods (not `CARD`)
- These bypass Stripe and mark payment as completed directly

## Security

### JWT Authentication

All customer-facing endpoints (except auth and public menu) require JWT token:

```
Authorization: Bearer {jwt-token}
```

### Internal API Security

⚠️ **Important**: The Internal API endpoints (`/api/internal/*`) are currently public to allow Admin Backend access.

**Production Recommendations:**
1. Add IP whitelisting for Admin Backend
2. Use API keys or OAuth for service-to-service auth
3. Place behind VPN or private network

## Monitoring and Logs

The application logs all integration activities:
- Data sync operations
- Admin API calls
- Stripe payment events
- WebSocket connections

Check logs for sync status:

```bash
tail -f logs/application.log | grep "DataSyncService"
```

## Troubleshooting

### Sync Issues

**Problem**: Categories/products not syncing

**Solutions:**
1. Check Admin API is running: `curl http://localhost:8081/api/categories`
2. Verify credentials in `application.properties`
3. Check logs: `grep "AdminApiClient" logs/application.log`
4. Try manual sync: `POST /api/sync/all`

### Payment Issues

**Problem**: Payment intent creation fails

**Solutions:**
1. Verify Stripe API key is correct
2. Check Stripe Dashboard for errors
3. Ensure order amount is greater than 0
4. Check logs: `grep "StripePaymentService" logs/application.log`

### WebSocket Issues

**Problem**: Real-time updates not working

**Solutions:**
1. Verify WebSocket connection: Use a WebSocket client to test `/ws`
2. Check allowed origins configuration
3. Ensure STOMP client is subscribing to correct topic

## Flutter Integration Example

### 1. Create Order

```dart
final response = await http.post(
  Uri.parse('http://api.example.com/api/orders'),
  headers: {
    'Authorization': 'Bearer $token',
    'Content-Type': 'application/json',
  },
  body: jsonEncode({
    'orderItems': [
      {'productId': 1, 'quantity': 2}
    ],
    'orderType': 'DELIVERY',
    'deliveryAddress': '123 Main St',
    'phoneNumber': '+1234567890'
  }),
);
```

### 2. Create Payment Intent

```dart
final response = await http.post(
  Uri.parse('http://api.example.com/api/orders/$orderId/payment-intent'),
  headers: {'Authorization': 'Bearer $token'},
);
final clientSecret = jsonDecode(response.body)['data']['clientSecret'];
```

### 3. Process Payment with Stripe

```dart
await Stripe.instance.confirmPayment(
  clientSecret,
  PaymentMethodParams.card(/* card details */),
);
```

### 4. WebSocket Connection

```dart
final channel = WebSocketChannel.connect(
  Uri.parse('ws://api.example.com/ws'),
);

// Subscribe to order updates
channel.sink.add(jsonEncode({
  'destination': '/app/api/orders/$orderId/subscribe',
}));

// Listen for updates
channel.stream.listen((message) {
  final orderUpdate = jsonDecode(message);
  // Update UI with new order status
});
```

## API Response Format

All API endpoints return consistent format:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { /* response data */ },
  "timestamp": "2024-01-15T10:30:00"
}
```

## License

[Your License Here]
