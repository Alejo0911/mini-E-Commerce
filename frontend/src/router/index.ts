import { createRouter, createWebHistory, type RouteRecordRaw} from "vue-router";
import CheckoutView from "../views/CheckoutView.vue";
import ProductCatalog from "../views/ProductCatalog.vue";
import { useOrderStore } from "../stores/orderStores";

const routes: RouteRecordRaw[] = [
    {
        path: "/",
        name: "catalog",
        component: ProductCatalog,
        meta: { title: "Catálogo de Productos" }
    },
    {
            path: "/checkout",
            name: "checkout",
            component: CheckoutView,
            meta: { title: "Checkout y Reserva de Stock", requiresActiveOrder: true },
            beforeEnter: (_to, _from, next) => {
                const orderStore = useOrderStore();

                // Si no hay ninguna orden en el store, redirige al catálogo
                if(!orderStore.currentOrder) {
                    next({ name: 'catalog' });
                    return;
                } 

                // Si la orden pendiente ya sobrepasó su TTL, actualizar su estado reactivo a EXPIRED
                if (
                    orderStore.currentOrder.status === "PENDING_PAYMENT" &&
                    new Date(orderStore.currentOrder.expiresAt).getTime() <= Date.now()
                ) {
                    orderStore.markAsExpired();
                }

                next();
            }
    },
    {
        path: "/:pathMatch(.*)*",
        redirect: { name: "catalog" }
    }
        
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

export default router;