import * as bfexPlugin from "@bfex/plugin";
import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import "./app.css";

// Vuetify
import "vuetify/styles";
import { createVuetify } from "vuetify";
import { routes } from "./routes";

Object.assign(globalThis, {
  bfexPlugin,
});

createApp(App)
  .use(createVuetify({}))
  .use(
    createRouter({
      history: createWebHashHistory(),
      routes: routes,
    })
  )
  .mount("#app");


