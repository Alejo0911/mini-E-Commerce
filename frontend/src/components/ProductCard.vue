<script setup lang="ts">
import type { Product } from '../types/Product';
import defaultProductImage from '../assets/placeholder-product.svg';

const props = defineProps<{
    product: Product;
    quantity: number;
    reserving: boolean;
}>();

const emit = defineEmits<{
    (e: 'update:quantity', value: number): void;
    (e: 'reserve'): void;
}>();

const handleImageError = (event: Event): void => {
    const target = event.target as HTMLImageElement;
    if (target && target.src !== defaultProductImage) {
        target.src = defaultProductImage;
    }
};

const stockAvailable = (): number => props.product.inventory?.stockAvailable ?? 0;

const onQuantityInput = (event: Event): void => {
    const raw = (event.target as HTMLInputElement).value;
    const parsed = parseInt(raw, 10);
    if (!isNaN(parsed)) {
        emit('update:quantity', parsed);
    }
};
</script>

<template>
    <div class="product-card">
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
                <strong :class="{ 'out-of-stock': stockAvailable() === 0 }">
                    {{ stockAvailable() }} Unidades
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
                    :value="quantity"
                    min="1"
                    :max="product.inventory.stockAvailable"
                    class="qty-input"
                    @input="onQuantityInput"
                />
            </div>
            <button
                @click="emit('reserve')"
                :disabled="reserving"
                class="btn-order"
            >
                {{ reserving ? 'Reservando...' : 'Comprar y Reservar' }}
            </button>
        </div>
        <div v-else class="sold-out-badge">Agotado Temporalmente</div>
    </div>
</template>

<style scoped>
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
</style>

