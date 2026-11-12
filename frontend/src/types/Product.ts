export interface Inventory {
    id: number;
    productId: number;
    stockAvailable: number;
    stockReserved: number;
    version: number;
}

export interface Product {
    id: number;
    name: string;
    description?: string;
    price: number;
    createdAt?: string;
    inventory?: Inventory;
}