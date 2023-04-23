import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import vuetify, { transformAssetUrls } from "vite-plugin-vuetify";

export default defineConfig({
  plugins: [
    vue({
      customElement: /^dweb\-/,
      template: { transformAssetUrls },
    }),
    vuetify({
      autoImport: true,
      styles: true,
    }),
  ],
});
