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
    }
}