<script setup lang="ts">
import { ref, computed, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useOrderStore } from '../stores/orderStores';
import { useProductStore } from '../stores/productStore';
import { getFriendlyErrorMessage } from '../utils/errorHandler';

const router = useRouter();
const orderStore = useOrderStore();
const productStore = useProductStore();

const remainingSeconds = ref<number>(0);
const localError = ref<string | null>(null);
let timerInterval: number | null = null;

const calculateRemainingTime = (): void => {
    if (!orderStore.currentOrder || orderStore.currentOrder.status !== 'PENDING_PAYMENT') return;

    const expiresAtTime = new Date(orderStore.currentOrder.expiresAt).getTime();
    const now = Date.now();
    const diff = Math.floor((expiresAtTime - now) / 1000);

    if (diff <= 0) {
        remainingSeconds.value = 0;
        if (timerInterval !== null) {
            clearInterval(timerInterval);
            timerInterval = null;
        }
        orderStore.markAsExpired();
    } else {
        remainingSeconds.value = diff;
    }
};

const formattedTime = computed<string>(() => {
    const minutes = Math.floor(remainingSeconds.value / 60);
    const seconds = remainingSeconds.value % 60;
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
});

const isExpired = computed<boolean>(() => {
    return remainingSeconds.value <= 0 && orderStore.currentOrder?.status === 'PENDING_PAYMENT';
});

watch(
    () => orderStore.currentOrder,
    (newOrder) => {
        if (timerInterval !== null) {
            clearInterval(timerInterval);
            timerInterval = null;
        }
        if (newOrder && newOrder.status === 'PENDING_PAYMENT') {
            calculateRemainingTime();
            timerInterval = window.setInterval(calculateRemainingTime, 1000);
        }
    },
    { immediate: true }
);

onUnmounted(() => {
    if (timerInterval !== null) {
        clearInterval(timerInterval);
        timerInterval = null;
    }
});

const handlePay = async (): Promise<void> => {
    if (!orderStore.currentOrder) return;
    localError.value = null;

    try {
        await orderStore.payOrder(orderStore.currentOrder.id);
        if (timerInterval !== null) {
            clearInterval(timerInterval);
            timerInterval = null;
        }
        // Refrescar inventario en el catálogo
        await productStore.fetchProducts();
    } catch (err: unknown) {
        localError.value = getFriendlyErrorMessage(err);
    }
};

const handleCancel = async (): Promise<void> => {
    if (!orderStore.currentOrder) return;
    localError.value = null;

    try {
        await orderStore.cancelOrder(orderStore.currentOrder.id);
        if (timerInterval !== null) {
            clearInterval(timerInterval);
            timerInterval = null;
        }
        await productStore.fetchProducts();
    } catch (err: unknown) {
        localError.value = getFriendlyErrorMessage(err);
    }
};

const handleBackToCatalog = (): void => {
    orderStore.clearCurrentOrder();
    router.push({ name: 'catalog' });
};
</script>

<template>
    <div class="checkout-container">
        <h2>Resumen de Orden y Reserva Temporal</h2>

        <template v-if="orderStore.currentOrder">
            <div class="order-card">
                <div class="order-header">
                    <span>Orden #{{ orderStore.currentOrder.id }}</span>
                    <span :class="['status-badge', orderStore.currentOrder.status.toLowerCase()]">
                        {{ orderStore.currentOrder.status }}
                    </span>
                </div>

                <div v-if="orderStore.isOrderPending" class="timer-section">
                    <p class="timer-label">Tiempo restante para completar el pago:</p>
                    <div class="timer-clock" :class="{ urgent: remainingSeconds < 60 }">
                        {{ formattedTime }}
                    </div>
                    <p class="timer-warning">
                        El stock está reservado temporalmente. Si el tiempo expira, la reserva se revertirá de forma atómica.
                    </p>
                </div>

                <div class="items-list">
                    <h3>Productos reservados:</h3>
                    <ul>
                        <li v-for="item in orderStore.currentOrder.items" :key="item.id">
                            <span>{{ item.product.name }} (x{{ item.quantity }})</span>
                            <span>${{ ((item.priceAtOrder ?? item.unitPrice ?? 0) * item.quantity).toFixed(2) }}</span>
                        </li>
                    </ul>
                </div>

                <div class="order-total">
                    <strong>Total a pagar:</strong>
                    <span>${{ Number(orderStore.currentOrder.totalAmount).toFixed(2) }}</span>
                </div>

                <div v-if="orderStore.isOrderPending && !isExpired" class="actions-group">
                    <button
                        @click="handlePay"
                        :disabled="orderStore.loading || isExpired"
                        class="btn-pay"
                    >
                        {{ orderStore.loading ? 'Procesando Pago...' : 'Confirmar y Pagar Ahora' }}
                    </button>
                    <button
                        @click="handleCancel"
                        :disabled="orderStore.loading"
                        class="btn-cancel"
                    >
                        Cancelar Reserva
                    </button>
                </div>

                <div v-if="orderStore.isOrderPaid" class="success-message">
                    ¡Pago exitoso! Tu orden ha sido confirmada y el inventario ha sido consolidado definitivamente.
                    <div class="mt-3">
                        <button @click="handleBackToCatalog" class="btn-primary">Volver al Catálogo</button>
                    </div>
                </div>

                <div v-else-if="orderStore.currentOrder.status === 'EXPIRED' || isExpired" class="expired-message">
                    La reserva ha expirado. El stock ha sido devuelto automáticamente al inventario.
                    <div class="mt-3">
                        <button @click="handleBackToCatalog" class="btn-primary">Volver al Catálogo</button>
                    </div>
                </div>

                <div v-else-if="orderStore.currentOrder.status === 'CANCELLED'" class="expired-message">
                    La reserva ha sido cancelada. El stock ha sido liberado.
                    <div class="mt-3">
                        <button @click="handleBackToCatalog" class="btn-primary">Volver al Catálogo</button>
                    </div>
                </div>
            </div>
        </template>

        <div v-else class="no-order">
            <p>No tienes ninguna orden activa en este momento.</p>
            <button @click="handleBackToCatalog" class="btn-primary">Ir al Catálogo</button>
        </div>

        <div v-if="localError" class="error-msg">{{ localError }}</div>
        <div v-if="orderStore.error" class="error-msg">{{ orderStore.error }}</div>
    </div>
</template>

<style scoped>
.checkout-container {
    max-width: 600px;
    margin: 2rem auto;
    padding: 2rem;
    background: #fdfdfd;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
    font-family: Arial, sans-serif;
}
.checkout-container h2 {
    color: #2c3e50;
    text-align: center;
    margin-bottom: 1.5rem;
}
.order-card {
    background: white;
    padding: 1.5rem;
    border-radius: 6px;
    border: 1px solid #e0e0e0;
}
.order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: bold;
    font-size: 1.15rem;
    margin-bottom: 1rem;
    color: #2c3e50;
}
.status-badge {
    padding: 0.3rem 0.8rem;
    border-radius: 12px;
    font-size: 0.8rem;
    color: white;
    text-transform: uppercase;
}
.status-badge.pending_payment { background: #f39c12; }
.status-badge.paid { background: #27ae60; }
.status-badge.expired { background: #c0392b; }
.status-badge.cancelled { background: #7f8c8d; }

.timer-section {
    background: #fff3cd;
    border: 1px solid #ffeeba;
    padding: 1.2rem;
    border-radius: 6px;
    text-align: center;
    margin: 1.2rem 0;
}
.timer-label {
    margin: 0;
    font-size: 0.9rem;
    color: #856404;
}
.timer-clock {
    font-size: 2.2rem;
    font-weight: bold;
    color: #d35400;
    margin: 0.4rem 0;
    font-family: 'Courier New', Courier, monospace;
}
.timer-clock.urgent {
    color: #c0392b;
    animation: pulse 1s infinite;
}
.timer-warning {
    font-size: 0.8rem;
    color: #856404;
    margin: 0;
}
.items-list {
    margin: 1.5rem 0;
}
.items-list h3 {
    font-size: 1rem;
    color: #34495e;
    margin-bottom: 0.8rem;
}
.items-list ul {
    list-style: none;
    padding: 0;
    margin: 0;
}
.items-list li {
    display: flex;
    justify-content: space-between;
    padding: 0.6rem 0;
    border-bottom: 1px solid #eee;
    font-size: 0.95rem;
}
.order-total {
    display: flex;
    justify-content: space-between;
    font-size: 1.25rem;
    margin: 1.5rem 0;
    padding-top: 1rem;
    border-top: 2px solid #e0e0e0;
    color: #2c3e50;
}
.actions-group {
    display: flex;
    flex-direction: column;
    gap: 0.8rem;
}
.btn-pay {
    width: 100%;
    background: #27ae60;
    color: white;
    border: none;
    padding: 0.8rem;
    font-size: 1rem;
    font-weight: bold;
    border-radius: 6px;
    cursor: pointer;
    transition: background 0.2s;
}
.btn-pay:hover { background: #219653; }
.btn-pay:disabled { background: #95a5a6; cursor: not-allowed; }

.btn-cancel {
    width: 100%;
    background: #e74c3c;
    color: white;
    border: none;
    padding: 0.6rem;
    font-size: 0.9rem;
    font-weight: bold;
    border-radius: 6px;
    cursor: pointer;
    transition: background 0.2s;
}
.btn-cancel:hover { background: #c0392b; }
.btn-cancel:disabled { background: #bdc3c7; cursor: not-allowed; }

.btn-primary {
    background: #3498db;
    color: white;
    border: none;
    padding: 0.6rem 1.2rem;
    font-size: 0.9rem;
    font-weight: bold;
    border-radius: 4px;
    cursor: pointer;
}
.btn-primary:hover { background: #2980b9; }

.success-message {
    color: #27ae60;
    font-weight: bold;
    text-align: center;
    margin-top: 1.5rem;
    padding: 1rem;
    background: #e8f8f0;
    border-radius: 6px;
}
.expired-message {
    color: #c0392b;
    font-weight: bold;
    text-align: center;
    margin-top: 1.5rem;
    padding: 1rem;
    background: #fde8e8;
    border-radius: 6px;
}
.no-order {
    text-align: center;
    color: #666;
    margin-top: 2rem;
}
.error-msg {
    color: #c0392b;
    background: #fde8e8;
    padding: 0.75rem;
    border-radius: 4px;
    border: 1px solid #f8b4b4;
    text-align: center;
    margin-top: 1rem;
}
.mt-3 {
    margin-top: 1rem;
}

@keyframes pulse {
    0% { opacity: 1; }
    50% { opacity: 0.4; }
    100% { opacity: 1; }
}
</style>
