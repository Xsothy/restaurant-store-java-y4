/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2025-11-08 12:15:04.

declare namespace endpoint {

    interface CategoryResponse {
        id: number;
        name: string;
        description: string;
        createdAt: Date;
        updatedAt: Date;
    }

    interface CustomerResponse {
        id: number;
        name: string;
        email: string;
        phone: string;
        address: string;
        createdAt: Date;
    }

    interface DeliveryResponse {
        id: number;
        orderId: number;
        driverName: string;
        driverPhone: string;
        vehicleInfo: string;
        status: DeliveryStatus;
        pickupTime: Date;
        estimatedArrivalTime: Date;
        actualDeliveryTime: Date;
        deliveryNotes: string;
        currentLocation: string;
        createdAt: Date;
        updatedAt: Date;
    }

    interface OrderResponse {
        id: number;
        customerId: number;
        customerName: string;
        status: OrderStatus;
        totalPrice: number;
        orderType: OrderType;
        deliveryAddress: string;
        phoneNumber: string;
        specialInstructions: string;
        createdAt: Date;
        updatedAt: Date;
        estimatedDeliveryTime: Date;
        orderItems: OrderItemResponse[];
    }

    interface ProductResponse {
        id: number;
        name: string;
        description: string;
        price: number;
        imageUrl: string;
        isAvailable: boolean;
        categoryId: number;
        categoryName: string;
    }

    interface OrderItemResponse {
        id: number;
        productId: number;
        productName: string;
        quantity: number;
        unitPrice: number;
        totalPrice: number;
        specialInstructions: string;
    }

    type DeliveryStatus = "PENDING" | "ASSIGNED" | "PICKED_UP" | "ON_THE_WAY" | "DELIVERED" | "CANCELLED";

    type OrderStatus = "PENDING" | "CONFIRMED" | "PREPARING" | "READY" | "OUT_FOR_DELIVERY" | "DELIVERED" | "CANCELLED";

    type OrderType = "DELIVERY" | "PICKUP" | "DINE_IN";

}
