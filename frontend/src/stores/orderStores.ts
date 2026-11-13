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
    const isOrderPending = computed<boolean>(() => currentOrder.value?.status === 'PENDING_PAYMENT');
    const isOrderPaid = computed<boolean>(() => currentOrder.value?.status =="PAID");
    const isOrderExpired = computed<boolean>(() => {
        if (!currentOrder.value) return false;
        if (currentOrder.value.status === "EXPIRED") return true;
        if (currentOrder.value.status === "PENDING_PAYMENT") {
            return new Date(currentOrder.value.expiresAt).getTime() <= Date.now();
        }
        return false;
    });
    const orderExpiresAt = computed<string | null>(() => currentOrder.value?.expiresAt ?? null);


    // Actions
    async function createOrder(request: CreateOrderRequest): Promise<Order> {
        loading.value = true;
        error.value = null;

        try {
            const order = await orderService.createOrder(request);
            currentOrder.value = order;
            return order;
        } catch(err: unknown) {
            const message = getFriendlyErrorMessage(err);
            error.value = message;
            throw err;
        } finally {
            loading.value = false;
        }

        
    }

    async function payOrder(orderId: number): Promise<Order> {
        loading.value = true;
        error.value = null;

        try {
            const updatedOrder = await orderService.payOrder(orderId);
            currentOrder.value = updatedOrder;
            return updatedOrder;
        } catch (err: unknown) {
            const message = getFriendlyErrorMessage(err);
            error.value = message;
            throw err;
        } finally {
            loading.value = false;
        } 
    }

    async function cancelOrder(orderId: number): Promise<Order> {
        loading.value = true;
        error.value = null;

        try {
            const cancelledOrder = await orderService.cancelOrder(orderId);
            currentOrder.value = null;
            return cancelledOrder;
        } catch (err: unknown) {
            const message = getFriendlyErrorMessage(err);
            error.value = message;
            throw err;
        } finally {
            loading.value = false;
        }
    }

    async function checkActivePendingOrder(userId: number = 1): Promise<Order | null> {
        loading.value = true;
        error.value = null;

        try {
            const activeOrder = await orderService.getActivePendingOrder(userId);
            currentOrder.value = activeOrder;
            return activeOrder;
        } catch (err: unknown) {
            currentOrder.value = null;
            return null;
        } finally {
            loading.value = false;
        }
    }

    async function fetchOrder(orderId: number): Promise<Order> {
        loading.value = true;
        error.value = null;

        try {
            const order = await orderService.getOrderById(orderId);
            currentOrder.value = order;
            return order;
        } catch (err: unknown) {
            const message = getFriendlyErrorMessage(err);
            error.value = message;
            throw err;
        } finally {
            loading.value = false;
        }
    }

    function markAsExpired(): void {
        if (currentOrder.value && currentOrder.value.status === "PENDING_PAYMENT") {
            currentOrder.value.status = "EXPIRED";
        }
    }

    function clearCurrentOrder(): void {
        currentOrder.value = null;
        error.value = null;
    }

    return {
        currentOrder,
        loading,
        error,
        isOrderPending,
        isOrderPaid,
        isOrderExpired,
        orderExpiresAt,
        createOrder,
        payOrder,
        cancelOrder,
        fetchOrder,
        checkActivePendingOrder,
        markAsExpired,
        clearCurrentOrder
    };
});

