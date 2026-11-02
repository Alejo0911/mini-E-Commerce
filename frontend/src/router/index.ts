import { createRouter, createWebHistory} from "vue-router";
import CheckoutView from "../views/CheckoutView.vue";
import ProductCatalog from "../views/ProductCatalog.vue";
import { useOrderStore } from "../stores/orderStores";

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: "/checkout",
            name: "checkout",
            component: CheckoutView,
            beforeEnter: (to, from, next) => {
                const orderStore = useOrderStore();

                if(!orderStore.currentOrder) {
                    next({ name: 'catalog' });
                } else {
                    next();
                }
            }
        },
        {
            path: "/",
            name: "catalog",
            component: ProductCatalog
        }
    ]
});

export default router;