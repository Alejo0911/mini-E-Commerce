import type { Product } from "../types/Product";
import api from "./api";

export const productService = {
    async getAllProducts(): Promise<Product[]> {
        const response = await api.get<Product[]>('/products');
        return response.data;
    },

    async getProductById(id: number): Promise<Product> {
        const response = await api.get<Product>(`/products/${id}`);
        return response.data;
    }
};

