// Composables
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/setting.html/',
    name:"setting",
    component: () => import(/* webpackChunkName: "setting" */ '../views/Setting.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes,
})

export default router
