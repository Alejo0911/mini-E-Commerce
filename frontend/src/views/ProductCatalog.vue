<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import type { Product } from '../types/Product';
import { useProductStore } from '../stores/productStore';
import { useOrderStore } from '../stores/orderStores';
import { getFriendlyErrorMessage } from '../utils/errorHandler';
import PendingOrderBanner from '../components/PendingOrderBanner.vue';
import ProductCard from '../components/ProductCard.vue';

const router = useRouter();
const productStore = useProductStore();
const orderStore = useOrderStore();

const localError = ref<string | null>(null);
const quantities = ref<Record<number, number>>({});

// Cargar catálogo de productos a través de productStore
const loadProducts = async (): Promise<void> => {
    localError.value = null;
    try {
        const data = await productStore.fetchProducts();
        data.forEach((p: Product) => {
            const max = p.inventory?.stockAvailable ?? 0;
            if (!(p.id in quantities.value) || quantities.value[p.id] < 1) {
                quantities.value[p.id] = 1;
            } else if (max > 0 && quantities.value[p.id] > max) {
                quantities.value[p.id] = max;
            }
        });
    } catch (err: unknown) {
        localError.value = getFriendlyErrorMessage(err);
    }
};

// Crear orden y activar la reserva temporal con TTL (o redirigir a orden pendiente existente)
const handleCreateOrder = async (productId: number): Promise<void> => {
    localError.value = null;
    const quantity = quantities.value[productId] || 1;

    try {
        await orderStore.createOrder({
            userId: 1, // ID de usuario por defecto
            items: [{ productId, quantity }]
        });
        // Refrescar inventario fresco del servidor tras la reserva
        await productStore.fetchProducts();
        await router.push({ name: 'checkout' });
    } catch (err: unknown) {
        localError.value = getFriendlyErrorMessage(err);
        await productStore.fetchProducts();
    }
};

// Acciones para gestionar la orden activa pendiente desde el aviso superior
const handleContinuePayment = (): void => {
    router.push({ name: 'checkout' });
};

const handleCancelPendingOrder = async (): Promise<void> => {
    if (!orderStore.currentOrder) return;
    localError.value = null;
    try {
        await orderStore.cancelOrder(orderStore.currentOrder.id);
        // Refrescar inmediatamente el catálogo para mostrar el stock restituido
        await loadProducts();
    } catch (err: unknown) {
        localError.value = getFriendlyErrorMessage(err);
    }
};

onMounted(async () => {
    await Promise.all([
        loadProducts(),
        orderStore.checkActivePendingOrder(1)
    ]);
});
</script>

<template>
    <div class="catalog-container">
        <header class="catalog-header">
            <h2>Catálogo de Productos con Reserva Temporal (TTL)</h2>
            <p class="subtitle">Selecciona un producto para iniciar el proceso de checkout y reservar stock en tiempo real.</p>
        </header>

        <!-- Aviso superior de orden activa pendiente de pago -->
        <PendingOrderBanner
            v-if="orderStore.currentOrder && orderStore.isOrderPending && !orderStore.isOrderExpired"
            :order-id="orderStore.currentOrder.id"
            :total-amount="Number(orderStore.currentOrder.totalAmount)"
            :loading="orderStore.loading"
            @continue="handleContinuePayment"
            @cancel="handleCancelPendingOrder"
        />

        <div v-if="productStore.loading" class="loading">Cargando catálogo desde el servidor...</div>
        <div v-if="localError" class="error-msg">{{ localError }}</div>
        <div v-if="orderStore.error" class="error-msg">{{ orderStore.error }}</div>

        <div class="products-grid" v-if="!productStore.loading">
            <ProductCard
                v-for="product in productStore.products"
                :key="product.id"
                :product="product"
                :quantity="quantities[product.id] ?? 1"
                :reserving="orderStore.loading"
                @update:quantity="quantities[product.id] = $event"
                @reserve="handleCreateOrder(product.id)"
            />
        </div>
    </div>
</template>

<style scoped>
.catalog-container {
    max-width: 960px;
    margin: 2rem auto;
    padding: 2rem;
    font-family: Arial, sans-serif;
}
.catalog-header {
    text-align: center;
    margin-bottom: 2rem;
}
.catalog-header h2 {
    color: #2c3e50;
    margin-bottom: 0.5rem;
}
.subtitle {
    color: #7f8c8d;
    font-size: 0.95rem;
}
.products-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 1.5rem;
}
.loading, .error-msg {
    text-align: center;
    margin: 1.5rem 0;
    font-weight: bold;
}
.error-msg {
    color: #c0392b;
    background: #fde8e8;
    padding: 0.75rem;
    border-radius: 4px;
    border: 1px solid #f8b4b4;
}
</style>
