import { createApp } from "vue";
import { createRouter, createWebHashHistory } from "vue-router";
import App from "./App.vue";
import "./app.css";

// Vuetify
import "@mdi/font/css/materialdesignicons.css"; // Ensure you are using css-loader
import { createVuetify } from "vuetify";
import "vuetify/styles";
import { routes } from "./routes";

createApp(App)
  .use(
    createVuetify({
      icons: {},
    })
  )
  .use(
    createRouter({
      history: createWebHashHistory(),
      routes: routes,
    })
  )
  .mount("#app");
