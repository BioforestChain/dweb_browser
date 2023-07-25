// Composables
import { createRouter, createWebHistory } from "vue-router";

const routes = [
  {
    path: "/:pathMatch(.*)*/address.html/",
    name: "setting",
    component: () => import(/* webpackChunkName: "setting" */ "../views/address.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
});

export default router;
