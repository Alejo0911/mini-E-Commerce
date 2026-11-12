import { defineStore } from "pinia";
import { ref, computed } from "vue";
import type { Product } from "../types/Product";
import { productService } from "../services/productService";
import { getFriendlyErrorMessage } from "../utils/errorHandler";

export const useProductStore = defineStore("product", () => {
    // State
    const products = ref<Product[]>([]);
    const loading = ref<boolean>(false);
    const error = ref<string | null>(null);
    const selectedProduct = ref<Product | null>(null);

    // Getters
    const availableProducts = computed<Product[]>(() =>
        products.value.filter((p: Product) => (p.inventory?.stockAvailable ?? 0) > 0)
    );

    const getProductById = computed(() => {
        return (id: number): Product | undefined => products.value.find((p: Product) => p.id === id);
    });

    // Actions
    async function fetchProducts(): Promise<Product[]> {
        loading.value = true;
        error.value = null;

        try {
            const data = await productService.getAllProducts();
            products.value = data;
            return data;
        } catch (err: unknown) {
            const message = getFriendlyErrorMessage(err);
            error.value = message;
            throw err;
        } finally {
            loading.value = false;
        }
    }

    async function fetchProductById(id: number): Promise<Product> {
        loading.value = true;
        error.value = null;

        try {
            const product = await productService.getProductById(id);
            selectedProduct.value = product;
            return product;
        } catch (err: unknown) {
            const message = getFriendlyErrorMessage(err);
            error.value = message;
            throw err;
        } finally {
            loading.value = false;
        }
    }

    function updateLocalStock(productId: number, stockAvailable: number, stockReserved: number): void {
        const product = products.value.find((p: Product) => p.id === productId);
        if (product && product.inventory) {
            product.inventory.stockAvailable = stockAvailable;
            product.inventory.stockReserved = stockReserved;
        }
    }

    return {
        products,
        loading,
        error,
        selectedProduct,
        availableProducts,
        getProductById,
        fetchProducts,
        fetchProductById,
        updateLocalStock
    };
});

