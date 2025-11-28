/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2025-11-28 23:57:04.

declare namespace endpoint {

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
        paymentStatus: PaymentStatus;
        paymentMethod: PaymentMethod;
        paymentPaidAt: Date;
        paymentTransactionId: string;
        deliveryStatus: DeliveryStatus;
        deliveryDriverName: string;
        deliveryDriverPhone: string;
        deliveryEstimatedArrivalTime: Date;
        deliveryActualDeliveryTime: Date;
        deliveryLatitude: number;
        deliveryLongitude: number;
        pickupStatus: PickupStatus;
        pickupCode: string;
        pickupReadyAt: Date;
        pickupWindowStart: Date;
        pickupWindowEnd: Date;
        pickupPickedUpAt: Date;
        pickupInstructions: string;
    }

    interface OrderItemResponse {
        id: number;
        productId: number;
        productName: string;
        productImageUrl: string;
        quantity: number;
        unitPrice: number;
        totalPrice: number;
        specialInstructions: string;
    }

    type OrderStatus = "PENDING" | "CONFIRMED" | "PREPARING" | "READY_FOR_PICKUP" | "READY_FOR_DELIVERY" | "OUT_FOR_DELIVERY" | "COMPLETED" | "CANCELLED";

    type OrderType = "DELIVERY" | "PICKUP" | "DINE_IN";

    type PaymentStatus = "PENDING" | "AWAITING_SESSION" | "AWAITING_WEBHOOK" | "CASH_PENDING" | "PROCESSING" | "COMPLETED" | "FAILED" | "CANCELLED" | "REFUNDED";

    type PaymentMethod = "CREDIT_CARD" | "DEBIT_CARD" | "PAYPAL" | "STRIPE" | "ABA_PAYWAY" | "CASH_ON_DELIVERY" | "BANK_TRANSFER";

    type DeliveryStatus = "PENDING" | "ASSIGNED" | "PICKED_UP" | "ON_THE_WAY" | "DELIVERED" | "CANCELLED";

    type PickupStatus = "AWAITING_CONFIRMATION" | "PREPARING" | "READY_FOR_PICKUP" | "COMPLETED" | "CANCELLED";

}
