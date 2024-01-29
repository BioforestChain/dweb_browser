import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";
import vuetify, { transformAssetUrls } from "vite-plugin-vuetify";

export default defineConfig({
  plugins: [
    vue({
      customElement: /^dweb\-/,
      template: {
        transformAssetUrls,
        compilerOptions: {
          isCustomElement: (tag) => tag.startsWith("dweb-"),
        }
      }
    }),
    vuetify({
      autoImport: true,
      styles: true,
    }),
  ],
  server: {
    port: 4399,
  },
});
