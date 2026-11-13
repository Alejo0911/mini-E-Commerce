import type { CreateOrderRequest, Order } from "../types/Order";
import api from "./api";

export const orderService = {
    async createOrder(request: CreateOrderRequest): Promise<Order> {
        const response = await api.post<Order>('/orders', request);
        return response.data;
    },

    async payOrder(orderId: number): Promise<Order> {
        const response = await api.patch<Order>(`/orders/${orderId}/pay`);
        return response.data;
    },

    async cancelOrder(orderId: number): Promise<Order> {
        const response = await api.post<Order>(`/orders/${orderId}/cancel`);
        return response.data;
    },

    async getOrderById(orderId: number): Promise<Order> {
        const response = await api.get<Order>(`/orders/${orderId}`);
        return response.data;
    },

    async getActivePendingOrder(userId: number = 1): Promise<Order | null> {
        const response = await api.get<Order | null>('/orders/pending', {
            params: { userId }
        });
        if (response.status === 204 || !response.data || !response.data.id) {
            return null;
        }
        return response.data;
    }
}