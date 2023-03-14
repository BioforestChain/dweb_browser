import * as bfexPlugin from "@bfex/plugin";
import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import "./app.css";

// Vuetify
import "vuetify/styles";
import { createVuetify } from "vuetify";

Object.assign(globalThis, {
  bfexPlugin,
});

createApp(App)
  .use(createVuetify({}))
  .use(
    createRouter({
      history: createWebHashHistory(),
      routes: [
        { path: "/haptics", component: () => import("./pages/Haptics.vue") },
        { path: "/keyboard", component: () => import("./pages/Keyboard.vue") },
        { path: "/navigationbar", component: () => import("./pages/NavigationBar.vue") },
        { path: "/safearea", component: () => import("./pages/SafeArea.vue") },
        { path: "/share", component: () => import("./pages/Share.vue") },
        { path: "/splashscreen", component: () => import("./pages/SplashScreen.vue") },
        { path: "/statusbar", component: () => import("./pages/StatusBar.vue") },
        { path: "/toast", component: () => import("./pages/Toast.vue") },
      ],
    })
  )
  .mount("#app");
