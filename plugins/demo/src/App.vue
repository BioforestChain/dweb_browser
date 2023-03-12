<script setup lang="ts">
import { ref } from "vue";

// import Haptics from './pages/Haptics.vue'
defineProps({ routes: Array });
// routes
// prop
const routes = [
  { path: "/haptics", title: "Haptics" },
  { path: "/keyboard", title: "Keyboard" },
  { path: "/navigationbar", title: "Navigation Bar" },
  { path: "/safearea", title: "Safe Area" },
  { path: "/share", title: "Share" },
  { path: "/splashscreen", title: "Splash Screen" },
  { path: "/statusbar", title: "Status Bar" },
  { path: "/toast", title: "Toast" },
];

const drawer_controller = ref(false);
</script>
<template>
  <div class="drawer h-full app-bg">
    <input id="my-drawer-controller" type="checkbox" class="drawer-toggle" v-model="drawer_controller" />
    <div class="drawer-content flex flex-col">
      <!-- Navbar -->
      <div class="navbar">
        <div class="flex-none">
          <label for="my-drawer-controller" class="btn btn-square btn-ghost">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              class="inline-block w-6 h-6 stroke-current"
            >
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </label>
        </div>
        <div class="flex-1">
          <h1 class="text-xl">JMM Plugins Demo</h1>
        </div>
        <div class="flex-none">
          <button class="btn btn-square btn-ghost">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              class="inline-block w-5 h-5 stroke-current"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M5 12h.01M12 12h.01M19 12h.01M6 12a1 1 0 11-2 0 1 1 0 012 0zm7 0a1 1 0 11-2 0 1 1 0 012 0zm7 0a1 1 0 11-2 0 1 1 0 012 0z"
              />
            </svg>
          </button>
        </div>
      </div>
      <!-- Page content here -->
      <!-- <article class="card w-96 bg-base-100 shadow-xl m-4 prose"> -->
      <!-- <h2 class="text-6xl font-bold opacity-[0.03] px-2 h-10">Toast</h2> -->
      <section class="p-4">
        <router-view></router-view>
      </section>
      <!-- </article> -->
    </div>
    <div class="drawer-side">
      <label for="my-drawer-controller" class="drawer-overlay" />
      <ul class="menu p-4 w-80 glass bg-base-100 bg-opacity-50">
        <!-- Sidebar content here -->
        <li v-for="route in routes" :key="route.path">
          <router-link
            :to="route.path"
            @click="
              () => {
                drawer_controller = false;
              }
            "
            >{{ route.title }}</router-link
          >
        </li>
      </ul>
    </div>
  </div>
</template>
<style scoped>
.app-bg {
  background: url(../assets/bg.png);
  background-size: cover;
  background-position: center;
  /* animation-name: ani-bg; */
  animation-duration: 60s;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
  animation-direction: alternate;
  animation-fill-mode: forwards;
}
@keyframes ani-bg {
  0% {
    background-position: top left;
  }
  100% {
    background-position: bottom right;
  }
  /* 100% {
    background-position: bottom right;
  } */
}
</style>
