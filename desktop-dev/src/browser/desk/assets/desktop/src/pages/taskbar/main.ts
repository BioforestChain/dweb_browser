/**
 * main.ts
 *
 * Bootstraps Vuetify and other plugins then mounts the App`
 */

// Components
import App from "./App.vue";

// Composables
import { createApp } from "vue";

// Plugins
import { registerPlugins } from "./plugins/index.ts";

const app = createApp(App);

registerPlugins(app);

app.mount("#app");

window.oncontextmenu = (event) => {
  event.preventDefault();
};

import { exportApis } from "./bridge-apis.ts";
// import { TaskbarElement } from "./taskbar.html.ts";

const taskbar = new TaskbarElement();
document.body.appendChild(taskbar);
exportApis(taskbar);
Object.assign(globalThis, { taskbar });
