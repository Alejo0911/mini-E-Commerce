import { createRouter, createWebHistory} from "vue-router";
import CheckoutView from "../views/CheckoutView.vue";

const router = createRouter({
    history: createWebHistory(),
    routes: [
        {
            path: "/chekout",
            name: "checkout",
            component: CheckoutView
        }
    ]
});

export default router;