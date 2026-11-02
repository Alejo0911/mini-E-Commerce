import { defineStore } from "pinia";
import { ref, computed } from 'vue'; 
import type { CreateOrderRequest, Order } from "../types/Order";
import { orderService } from "../services/orderService";
import { getFriendlyErrorMessage } from "../utils/errorHandler";

export const useOrderStore = defineStore('order', () => {

    // State
    const currentOrder = ref<Order | null>(null);
    const loading = ref<boolean>(false);
    const error = ref<string | null>(null);

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
        } catch(err: unknown) {
            error.value = getFriendlyErrorMessage(err);
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
        } catch (err: unknown) {
            error.value = getFriendlyErrorMessage(err);
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

