<script setup lang="ts">
defineProps<{
    orderId: number;
    totalAmount: number;
    loading: boolean;
}>();

const emit = defineEmits<{
    (e: 'continue'): void;
    (e: 'cancel'): void;
}>();
</script>

<template>
    <div class="pending-order-banner">
        <div class="banner-body">
            <div class="banner-icon">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <polyline points="12 6 12 12 16 14"></polyline>
                </svg>
            </div>
            <div class="banner-info">
                <h4>Tienes una reserva activa pendiente de pago (Orden #{{ orderId }})</h4>
                <p>
                    Total reservado: <strong>${{ totalAmount.toFixed(2) }}</strong>.
                    Completa tu pago antes de que expire el tiempo o cancela tu reserva para devolver el inventario.
                </p>
            </div>
        </div>
        <div class="banner-actions">
            <button @click="emit('continue')" class="btn-banner-continue">
                Continuar con el pago
            </button>
            <button
                @click="emit('cancel')"
                :disabled="loading"
                class="btn-banner-cancel"
            >
                {{ loading ? 'Cancelando...' : 'Cancelar reserva' }}
            </button>
        </div>
    </div>
</template>

<style scoped>
.pending-order-banner {
    background: #fff8e6;
    border: 1px solid #f6d58d;
    border-left: 5px solid #e67e22;
    border-radius: 8px;
    padding: 1.25rem 1.5rem;
    margin-bottom: 2rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1.5rem;
    box-shadow: 0 2px 8px rgba(230, 126, 34, 0.08);
}
.banner-body {
    display: flex;
    align-items: center;
    gap: 1rem;
    flex: 1;
}
.banner-icon {
    color: #e67e22;
    display: flex;
    align-items: center;
    flex-shrink: 0;
}
.banner-info h4 {
    margin: 0 0 0.25rem 0;
    color: #9c5200;
    font-size: 1.05rem;
}
.banner-info p {
    margin: 0;
    color: #7d4400;
    font-size: 0.9rem;
}
.banner-actions {
    display: flex;
    gap: 0.75rem;
    align-items: center;
    flex-wrap: wrap;
    flex-shrink: 0;
}
.btn-banner-continue {
    background: #27ae60;
    color: white;
    border: none;
    padding: 0.6rem 1.2rem;
    border-radius: 6px;
    font-weight: bold;
    font-size: 0.9rem;
    cursor: pointer;
    transition: background 0.2s, transform 0.1s;
}
.btn-banner-continue:hover {
    background: #219653;
    transform: translateY(-1px);
}
.btn-banner-cancel {
    background: transparent;
    color: #c0392b;
    border: 1px solid #e74c3c;
    padding: 0.55rem 1.1rem;
    border-radius: 6px;
    font-weight: bold;
    font-size: 0.9rem;
    cursor: pointer;
    transition: all 0.2s;
}
.btn-banner-cancel:hover:not(:disabled) {
    background: #fadbd8;
}
.btn-banner-cancel:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}
@media (max-width: 640px) {
    .pending-order-banner {
        flex-direction: column;
        align-items: flex-start;
    }
    .banner-actions {
        width: 100%;
        justify-content: flex-end;
    }
}
</style>

