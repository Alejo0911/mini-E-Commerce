<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useOrderStore } from '../stores/orderStores';

const orderStore = useOrderStore();
const remainingSeconds = ref<number>(0);
let timerInterval: number | null = null;

const calculateRemainingTime = () => {
    if (!orderStore.currentOrder || orderStore.currentOrder.status !== 'PENDING_PAYMENT') return;
    
    const expiresAtTime = new Date(orderStore.currentOrder.expiresAt).getTime();
    const now = new Date().getTime();
    const diff = Math.floor((expiresAtTime - now) / 1000);

    if (diff <= 0) {
        remainingSeconds.value = 0;
        if (timerInterval) clearInterval(timerInterval);
        
        // IDEAL: Llamar a una acción del store en lugar de mutar directamente
        // orderStore.expireOrder();
        if (orderStore.currentOrder.status === 'PENDING_PAYMENT') {
            orderStore.currentOrder.status = 'EXPIRED';
        }
    } else {
        remainingSeconds.value = diff;
    }
};

const formattedTime = computed(() => {
    const minutes = Math.floor(remainingSeconds.value / 60);
    const seconds = remainingSeconds.value % 60;
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
});

const isExpired = computed(() => remainingSeconds.value <= 0 && orderStore.currentOrder?.status === 'PENDING_PAYMENT');

// Manejar el ciclo del timer de forma reactiva por si la orden carga de forma asíncrona
watch(
    () => orderStore.currentOrder,
    (newOrder) => {
        if (timerInterval) clearInterval(timerInterval);
        if (newOrder && newOrder.status === 'PENDING_PAYMENT') {
            calculateRemainingTime();
            timerInterval = window.setInterval(calculateRemainingTime, 1000);
        }
    },
    { immediate: true }
);

onUnmounted(() => {
    if (timerInterval) clearInterval(timerInterval);
});

const handlePay = async () => {
    if (!orderStore.currentOrder) return;

    try {
        await orderStore.payOrder(orderStore.currentOrder.id);
        if (timerInterval) clearInterval(timerInterval);
    } catch (err) {
        console.error('Error al pagar la orden', err); 
    }
};
</script>

<template>
    <div class="checkout-container">
        <h2>Resumen de Orden y reserva temporal</h2>
        <template v-if="orderStore.currentOrder">
            <div class="order-card">
                <div class="order-header">
                    <span>Orden # {{ orderStore.currentOrder.id }}</span>
                    <span :class="['status-badge', orderStore.currentOrder.status.toLowerCase()]">
                        {{ orderStore.currentOrder.status }}
                    </span>
                </div>
            </div>
            <div v-if="orderStore.isOrderPending" class="timer-section">
                <p class="timer-label">Tiempo restante para completar el pago:</p>
                <div class="timer-clock" :class="{ urgent: remainingSeconds < 60 }">
                    {{ formattedTime }}
                </div>
                <p class="timer-warning">El stock está reservado temporalmente. Si el tiempo expira, la orden se cancelará automáticamente.</p>
            </div>

            <div class="items-list">
                <h3>Productos en la orden:</h3>
                <ul>
                    <li v-for="item in orderStore.currentOrder.items" :key="item.id">
                        <span>{{ item.product.name }} (x{{ item.quantity }})</span>
                        <span>${{ item.priceAtOrder * item.quantity }}</span>
                    </li>
                </ul>
            </div>

            <div class="order-total">
                <strong>Total a pagar:</strong>
                <span>${{ orderStore.currentOrder.totalAmount }}</span>
            </div>

            <div v-if="orderStore.isOrderPending" class="actions">
                <button @click="handlePay" :disabled="orderStore.loading || isExpired" class="btn-pay">
                    {{ orderStore.loading ? 'Procesando...' : 'Pagar Ahora' }}
                </button>
            </div>
            <div v-if="orderStore.currentOrder.status === 'PAID'" class="success-message">
                ¡Pago exitoso! Tu orden ha sido procesada y el stock consolidado.
            </div>
            <div v-else-if="orderStore.currentOrder.status === 'EXPIRED' || isExpired" class="expired-message">
                La orden ha expirado. El stock ha sido devuelto al inventario.
            </div>
        </template>
        <div v-else class="no-order">
            <p>No tienes ninguna orden activa en este momento.</p>
        </div>

        <p v-if="orderStore.error" class="error-msg">{{ orderStore.error }}</p>
    </div>
</template>

<style>
.checkout-container {
    max-width: 600px;
    margin: 2rem auto;
    padding: 2rem;
    background: #f9f9f9;
    border-radius: 8px;
    box-shadow: 0 4px 6px rgba(0,0,0,0.1);
    font-family: Arial, sans-serif;
}
.order-card {
    background: white;
    padding: 1.5rem;
    border-radius: 6px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}
.order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: bold;
    font-size: 1.2rem;
    margin-bottom: 1rem;
}
.status-badge {
    padding: 0.25rem 0.75rem;
    border-radius: 12px;
    font-size: 0.85rem;
    color: white;
}
.status-badge.pending_payment { background: #f39c12; }
.status-badge.paid { background: #27ae60; }
.status-badge.expired { background: #c0392b; }

.timer-section {
    background: #fff3cd;
    border: 1px solid #ffeeba;
    padding: 1rem;
    border-radius: 6px;
    text-align: center;
    margin: 1rem 0;
}
.timer-clock {
    font-size: 2rem;
    font-weight: bold;
    color: #d35400;
    margin: 0.5rem 0;
}
.timer-clock.urgent {
    color: #c0392b;
    animation: pulse 1s infinite;
}
.timer-warning {
    font-size: 0.85rem;
    color: #856404;
}
.items-list ul {
    list-style: none;
    padding: 0;
}
.items-list li {
    display: flex;
    justify-content: space-between;
    padding: 0.5rem 0;
    border-bottom: 1px solid #eee;
}
.order-total {
    display: flex;
    justify-content: space-between;
    font-size: 1.2rem;
    margin: 1.5rem 0;
}
.btn-pay {
    width: 100%;
    background: #27ae60;
    color: white;
    border: none;
    padding: 0.75rem;
    font-size: 1rem;
    font-weight: bold;
    border-radius: 4px;
    cursor: pointer;
}
.btn-pay:hover { background: #219653; }
.btn-pay:disabled { background: #95a5a6; cursor: not-allowed; }
.success-message { color: #27ae60; font-weight: bold; text-align: center; margin-top: 1rem; }
.expired-message { color: #c0392b; font-weight: bold; text-align: center; margin-top: 1rem; }
.no-order { text-align: center; color: #666; margin-top: 1rem; }
.error-msg { color: #c0392b; text-align: center; margin-top: 1rem; }

@keyframes pulse {
    0% { opacity: 1; }
    50% { opacity: 0.5; }
    100% { opacity: 1; }
}
</style>