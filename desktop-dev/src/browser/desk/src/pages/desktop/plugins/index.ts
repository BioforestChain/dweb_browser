/**
 * plugins/index.ts
 *
 * Automatically included in `./src/main.ts`
 */

// Plugins
import router from "../router/index.ts";
import vuetify from "./vuetify.ts";

// Types
import type { App } from "vue";

export function registerPlugins(app: App) {
  // loadFonts()
  app
    // use plugins
    .use(vuetify)
    .use(router);
}
