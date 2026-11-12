import type { Product } from "./Product";

export interface OrderItemRequest {
    productId: number;
    quantity: number;
}

export interface CreateOrderRequest {
    userId: number;
    items: OrderItemRequest[];
}

export interface OrderItem {
    id: number;
    product: Product;
    quantity: number;
    priceAtOrder: number;
    unitPrice?: number;
}

export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'EXPIRED' | 'CANCELLED';

export interface Order {
    id: number;
    userId: number;
    status: OrderStatus;
    totalAmount: number;
    expiresAt: string;
    createdAt: string;
    items: OrderItem[];
}