<script setup lang="ts">
import { ref } from 'vue';
import type { Product } from '../types/Product';
import { useOrderStore } from '../stores/orderStores';
import { useRouter } from 'vue-router';
import api from '../services/api';
import { onMounted } from 'vue';

const router = useRouter();
const orderStore = useOrderStore();

const loading = ref<boolean>(false);
const error = ref<string | null>(null);
const products = ref<Product[]>([]);
const quantities = ref<Record<number, number>>({});

// Obtener productos y su inventario desde el backend
const fetchProducts = async () => {
    loading.value = true;
    error.value = null;

    try {
        const response = await api.get<Product[]>('/products');
        products.value = response.data;

        products.value.forEach((p: Product) => {
            quantities.value[p.id] = 1;
        });
    } catch(err: any) {
        console.error(err);
        error.value = 'No se pudo cargar el catálogo. Inténtate más tarde.';
        loading.value = false;
    }
};

// Crear orden y activar la reserva temporal (TTL)
const handleCreateOrder = async (productId: number) => {
    const quantity = quantities.value[productId] || 1;

    try {
        await orderStore.createOrder({
            userId: 1,
            items: [
                { productId, quantity }
            ]
        });
        router.push('/checkout');
    } catch (err) {
    console.error('Error al crear la orden con reserva de stock', err);
    }
};

onMounted(() => {
    fetchProducts();
})
</script>

<template>
    <div class="catalog-container">
        <h2>Catálogo de Productos y Stock</h2>
        <div v-if="loading" class="loading">Cargando catálogo...</div>
        <div v-if="error" class="error-msg">{{ error }}</div>
        <div class="products-grid" v-if="!loading">
            <div v-for="product in products" :key="product.id" class="product-card">
                <div class="product-info">
                    <h3>{{ product.name }}</h3>
                    <p class="description">{{ product.description || 'Sin descripción' }}</p>
                    <p class="price">${{ product.price.toFixed(2) }}</p>
                    <div class="stock-info">
                        <span>Stock disponible:</span>
                        <strong :class="{ 'out-of-stock': (product.inventory?.stockAvailable ?? 0) === 0 }">
                            {{ product.inventory?.stockAvailable ?? 0 }} Unidades
                        </strong>
                    </div>
                </div>
                <div class="product-actions" v-if="product.inventory && product.inventory.stockAvailable > 0">
                    <div class="quantity-selector">
                        <label for="qty">Cant:</label>
                        <input 
                        id="qty"
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
                    {{ orderStore.loading? 'Reservando...' : 'Comprar y Reservar' }}    
                    </button>
                </div>
                <div v-else class="sold-out-badge">Agotado Temporalmente</div>
            </div>
        </div>
        <p v-if="orderStore.error" class="error-msg">{{ orderStore.error }}</p>
    </div>
</template>

<style scoped>
.catalog-container {
    max-width: 900px;
    margin: 2rem auto;
    padding: 2rem;
    font-family: Arial, sans-serif;
}
.products-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 1.5rem;
    margin-top: 1.5rem;
}
.product-card {
    background: white;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    padding: 1.5rem;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}
.product-info h3 {
    margin: 0 0 0.5rem 0;
    font-size: 1.2rem;
    color: #2c3e50;
}
.description {
    font-size: 0.9rem;
    color: #7f8c8d;
    margin-bottom: 1rem;
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
    width: 50px;
    padding: 0.3rem;
    border: 1px solid #ccc;
    border-radius: 4px;
    text-align: center;
}
.btn-order {
    flex: 1;
    background: #3498db;
    color: white;
    border: none;
    padding: 0.5rem;
    border-radius: 4px;
    font-weight: bold;
    cursor: pointer;
    font-size: 0.85rem;
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
    margin: 2rem 0;
    font-weight: bold;
}
.error-msg { color: #c0392b; }
</style>