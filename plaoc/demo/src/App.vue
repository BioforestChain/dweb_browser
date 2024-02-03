<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import * as PLAOC from "./plugin";
import { routes } from "./routes";

const router = useRouter();
onMounted(() => {
  Object.assign(globalThis, { PLAOC });
});
// router.push("/shortcut");
// router.push("/biometrics")
// router.push("/share")
// router.push("/webview");
// router.push("/splashscreen")
// router.push("/serviceworker");
// router.push("/shortcut");
// router.push("/inputfile");
// router.push("/barcodescanning");
// router.push("/network");
// router.push("/safearea");
// router.push("/haptics");
// router.push("/closewatcher")
// router.push("/statusbar")
// router.push("/window")
// router.push("motionSensors")

// dwebServiceWorker.addEventListener("pause", (event) => {
//   console.log("app暂停🍋", event);
// });

// dwebServiceWorker.addEventListener("resume", (event) => {
//   console.log("app 恢复🍉", event);
// });

const drawer_controller = ref(false);

// const apiUrl = new URL(location.href);
// {
//   apiUrl.hostname = apiUrl.hostname.replace("www", "api");
//   const xDwebHost = apiUrl.searchParams.get("X-Dweb-Host");
//   if (xDwebHost) {
//     apiUrl.searchParams.set("X-Dweb-Host", xDwebHost.replace("www", "api"));
//   }
//  // https://api.cotdemo.bfs.dweb/index.html?X-Dweb-Host=api.cotdemo.bfs.dweb%3A443#/
// }
</script>
<template>
  <v-app>
    <v-main>
      <div class="h-full drawer app-bg pt-10">
        <input id="my-drawer-controller" type="checkbox" class="drawer-toggle" v-model="drawer_controller" />
        <div class="flex flex-col drawer-content">
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
          <!-- <article class="m-4 prose shadow-xl card w-96 bg-base-100"> -->
          <!-- <h2 class="text-6xl font-bold opacity-[0.03] px-2 h-10">Toast</h2> -->
          <section class="p-4">
            <router-view></router-view>
          </section>
          <!-- </article> -->
        </div>
        <div class="drawer-side">
          <label for="my-drawer-controller" class="drawer-overlay" />
          <ul class="p-4 bg-opacity-50 menu w-80 glass bg-base-100 rounded-lg">
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
    </v-main>
  </v-app>
</template>
<style scoped>
/* 隐藏滚动条 */
html {
  -ms-overflow-style: none;
  overflow: -moz-scrollbars-none;
}
html::-webkit-scrollbar {
  width: 0px;
}
/* 隐藏滚动条结束 */
.app-bg {
  background: url("../assets/bg.png");
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
