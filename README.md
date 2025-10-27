# Restaurant Store API

A Spring Boot REST API for a restaurant ordering system that serves as the backend for Flutter mobile applications.

## Project Overview

This API provides endpoints for:
- Customer registration and authentication (JWT-based)
- Menu browsing (categories and products)
- Order placement and management
- Payment processing
- Delivery tracking
- Order history

## Technology Stack

- **Backend**: Spring Boot 3.2.0
- **Database**: SQLite (for development)
- **ORM**: Spring Data JPA with Hibernate
- **Authentication**: JWT (JSON Web Tokens)
- **Validation**: Bean Validation (Jakarta)
- **Build Tool**: Maven

## Project Structure

```
src/
├── main/
│   ├── java/com/restaurant/store/
│   │   ├── controller/          # REST Controllers
│   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── request/         # Request DTOs
│   │   │   └── response/        # Response DTOs
│   │   ├── entity/              # JPA Entities
│   │   ├── repository/          # Data Access Layer
│   │   └── RestaurantStoreApiApplication.java
│   └── resources/
│       ├── application.properties
│       └── data.sql             # Sample data
└── test/
```

## Database Schema

### Core Entities
- **Customer**: User accounts and profiles
- **Category**: Food categories (Appetizers, Main Courses, etc.)
- **Product**: Menu items with pricing and availability
- **Order**: Customer orders with status tracking
- **OrderItem**: Individual items within orders
- **Payment**: Payment processing records
- **Delivery**: Delivery tracking information

## API Endpoints

### Authentication
- `POST /api/auth/register` - Customer registration
- `POST /api/auth/login` - Customer login
- `POST /api/auth/logout` - Customer logout

### Menu Browsing
- `GET /api/categories` - Get all food categories
- `GET /api/products` - Get all products (with optional filters)
- `GET /api/products/{id}` - Get specific product
- `GET /api/categories/{id}/products` - Get products by category

### Order Management
- `POST /api/orders` - Create new order
- `GET /api/orders/{id}` - Get order details
- `GET /api/orders/{id}/status` - Get order status
- `POST /api/orders/{id}/pay` - Process payment
- `GET /api/orders/my-orders` - Get current user's orders
- `PUT /api/orders/{id}/cancel` - Cancel order

### Delivery Tracking
- `GET /api/deliveries/{orderId}` - Get delivery info by order
- `GET /api/deliveries/track/{orderId}` - Track delivery status

## Setup Instructions

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Running the Application

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd restaurant-store-api
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the API**
   - Base URL: `http://localhost:8080/api`
   - Database file: `restaurant_store.db` (created automatically)

### Sample Data

The application includes sample data with:
- 5 food categories
- 14 menu items
- 2 sample customers
- 2 sample orders with delivery tracking

### Database Configuration

The application uses SQLite for development with the following configuration:
- Database file: `restaurant_store.db`
- DDL mode: `create-drop` (recreates schema on startup)
- Show SQL: Enabled for debugging

## Development Notes

### Current Status
✅ **Completed:**
- Project structure setup
- JPA entities with relationships
- Request/Response DTOs
- Repository interfaces
- REST controller stubs
- Database configuration
- Sample data

🚧 **Next Steps (for your teammate):**
- Service layer implementation
- JWT authentication logic
- Payment gateway integration
- Validation logic
- Exception handling
- Unit tests

### Key Features Ready for Implementation
- All database models are defined with proper relationships
- REST endpoints are stubbed and ready for service integration
- DTOs are created for all request/response scenarios
- Repository methods are defined for data access

### Security Notes
- JWT secret is configured in `application.properties`
- Password hashing should use BCrypt
- All endpoints (except auth) require JWT token in Authorization header

## API Response Format

All endpoints return responses in this format:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {...},
  "timestamp": "2024-01-15T10:30:00"
}
```

## Contributing

When implementing services:
1. Create service classes in `src/main/java/com/restaurant/store/service/`
2. Inject repositories into services
3. Update controller methods to use services
4. Add proper exception handling
5. Write unit tests

The foundation is solid - your teammate can focus on business logic implementation!