/**
 * main.ts
 *
 * Bootstraps Vuetify and other plugins then mounts the App`
 */

// Components
import "../../provider/disable-context-menu.ts";
import App from "./App.vue";

// Composables
import { createApp } from "vue";

// Plugins
import { registerPlugins } from "./plugins/index.ts";

const app = createApp(App);

registerPlugins(app);

app.mount("#app");
