<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import type { Product } from '../types/Product';
import { useProductStore } from '../stores/productStore';
import { useOrderStore } from '../stores/orderStores';
import { getFriendlyErrorMessage } from '../utils/errorHandler';
import defaultProductImage from '../assets/placeholder-product.svg';

const router = useRouter();
const productStore = useProductStore();
const orderStore = useOrderStore();

const localError = ref<string | null>(null);
const quantities = ref<Record<number, number>>({});

const handleImageError = (event: Event): void => {
    const target = event.target as HTMLImageElement;
    if (target && target.src !== defaultProductImage) {
        target.src = defaultProductImage;
    }
};

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
        <div
            v-if="orderStore.currentOrder && orderStore.isOrderPending && !orderStore.isOrderExpired"
            class="pending-order-banner"
        >
            <div class="banner-body">
                <div class="banner-icon">
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"></circle>
                        <polyline points="12 6 12 12 16 14"></polyline>
                    </svg>
                </div>
                <div class="banner-info">
                    <h4>Tienes una reserva activa pendiente de pago (Orden #{{ orderStore.currentOrder.id }})</h4>
                    <p>
                        Total reservado: <strong>${{ Number(orderStore.currentOrder.totalAmount).toFixed(2) }}</strong>.
                        Completa tu pago antes de que expire el tiempo o cancela tu reserva para devolver el inventario.
                    </p>
                </div>
            </div>
            <div class="banner-actions">
                <button @click="handleContinuePayment" class="btn-banner-continue">
                    Continuar con el pago
                </button>
                <button
                    @click="handleCancelPendingOrder"
                    :disabled="orderStore.loading"
                    class="btn-banner-cancel"
                >
                    {{ orderStore.loading ? 'Cancelando...' : 'Cancelar reserva' }}
                </button>
            </div>
        </div>

        <div v-if="productStore.loading" class="loading">Cargando catálogo desde el servidor...</div>
        <div v-if="localError" class="error-msg">{{ localError }}</div>
        <div v-if="orderStore.error" class="error-msg">{{ orderStore.error }}</div>

        <div class="products-grid" v-if="!productStore.loading">
            <div v-for="product in productStore.products" :key="product.id" class="product-card">
                <div class="product-image-container">
                    <img
                        :src="product.imageUrl || defaultProductImage"
                        :alt="product.name"
                        class="product-image"
                        loading="lazy"
                        @error="handleImageError"
                    />
                </div>
                <div class="product-info">
                    <h3>{{ product.name }}</h3>
                    <p class="description">{{ product.description || 'Sin descripción' }}</p>
                    <p class="price">${{ product.price.toFixed(2) }}</p>
                    <div class="stock-info">
                        <span>Stock disponible:</span>
                        <strong :class="{ 'out-of-stock': (product.inventory?.stockAvailable ?? 0) === 0 }">
                            {{ product.inventory?.stockAvailable ?? 0 }} Unidades
                        </strong>
                        <span v-if="(product.inventory?.stockReserved ?? 0) > 0" class="reserved-info">
                            ({{ product.inventory?.stockReserved }} reservadas)
                        </span>
                    </div>
                </div>

                <div class="product-actions" v-if="product.inventory && product.inventory.stockAvailable > 0">
                    <div class="quantity-selector">
                        <label :for="'qty-' + product.id">Cant:</label>
                        <input
                            :id="'qty-' + product.id"
                            type="number"
                            v-model.number="quantities[product.id]"
                            min="1"
                            :max="product.inventory.stockAvailable"
                            class="qty-input"
                        />
                    </div>
                    <button
                        @click="handleCreateOrder(product.id)"
                        :disabled="orderStore.loading"
                        class="btn-order"
                    >
                        {{ orderStore.loading ? 'Reservando...' : 'Comprar y Reservar' }}
                    </button>
                </div>
                <div v-else class="sold-out-badge">Agotado Temporalmente</div>
            </div>
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
.products-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 1.5rem;
}
.product-card {
    background: white;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    box-shadow: 0 2px 4px rgba(0,0,0,0.04);
    transition: transform 0.2s, box-shadow 0.2s;
}
.product-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.product-image-container {
    width: 100%;
    height: 180px;
    border-radius: 6px;
    overflow: hidden;
    background-color: #f8fafc;
    margin-bottom: 1rem;
    display: flex;
    align-items: center;
    justify-content: center;
}
.product-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    transition: transform 0.3s ease;
}
.product-card:hover .product-image {
    transform: scale(1.03);
}
.product-info h3 {
    margin: 0 0 0.5rem 0;
    font-size: 1.15rem;
    color: #2c3e50;
}
.description {
    font-size: 0.85rem;
    color: #7f8c8d;
    margin-bottom: 1rem;
    min-height: 2.5rem;
}
.price {
    font-size: 1.3rem;
    font-weight: bold;
    color: #27ae60;
    margin-bottom: 0.5rem;
}
.stock-info {
    font-size: 0.85rem;
    color: #34495e;
    margin-bottom: 1rem;
}
.reserved-info {
    font-size: 0.75rem;
    color: #e67e22;
    margin-left: 0.3rem;
}
.out-of-stock {
    color: #c0392b;
}
.product-actions {
    display: flex;
    gap: 0.5rem;
    align-items: center;
    margin-top: 1rem;
}
.quantity-selector {
    display: flex;
    align-items: center;
    gap: 0.3rem;
    font-size: 0.85rem;
}
.qty-input {
    width: 55px;
    padding: 0.35rem;
    border: 1px solid #ccc;
    border-radius: 4px;
    text-align: center;
}
.btn-order {
    flex: 1;
    background: #3498db;
    color: white;
    border: none;
    padding: 0.55rem;
    border-radius: 4px;
    font-weight: bold;
    cursor: pointer;
    font-size: 0.9rem;
    transition: background 0.2s;
}
.btn-order:hover { background: #2980b9; }
.btn-order:disabled { background: #bdc3c7; cursor: not-allowed; }
.sold-out-badge {
    text-align: center;
    background: #fadbd8;
    color: #c0392b;
    padding: 0.5rem;
    border-radius: 4px;
    font-weight: bold;
    font-size: 0.85rem;
    margin-top: 1rem;
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
