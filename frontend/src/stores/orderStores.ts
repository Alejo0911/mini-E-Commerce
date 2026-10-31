import { defineStore } from "pinia";
import { computed } from 'vue'; 
import { ref as vueRef } from 'vue';
import type { CreateOrderRequest, Order } from "../types/Order";
import { orderService } from "../services/orderService";

export const useOrderStore = defineStore('order', () => {

    // State
    const currentOrder = vueRef<Order | null>(null);
    const loading = vueRef<boolean>(false);
    const error = vueRef<string | null>(null);

    // Getters / Computed
    const isOrderPending = computed(() => currentOrder.value?.status === 'PENDING_PAYMENT');
    const orderExpiresAt = computed(() => currentOrder.value?.expiresAt || null);

    // Actions
    async function createOrder(request: CreateOrderRequest) {
        loading.value = true;
        error.value = null;

        try {
            const order = await orderService.createOrder(request);
            currentOrder.value = order;
            return order;
        } catch(err: any) {
            error.value = err.response?.data?.message || 'Error al crear la orden con reserva temporal.';
            throw err;
        } finally {
            loading.value = false;
        }

        
    }

    async function payOrder(orderId: number) {
        loading.value = true;
        error.value = null;

        try {
            const updatedOrder = await orderService.payOrder(orderId);
            currentOrder.value = updatedOrder;
            return updatedOrder;
        } catch (err: any) {
            error.value = err.response?.data?.message || 'Error al procesar el pago de la orden.';
            throw err;
        } finally {
            loading.value = false;
        } 
    }

    function clearCurrentOrder() {
        currentOrder.value = null;
        error.value = null;
    }

    return {
        currentOrder,
        loading,
        error,
        isOrderPending,
        orderExpiresAt,
        createOrder,
        payOrder,
        clearCurrentOrder
    };
});

