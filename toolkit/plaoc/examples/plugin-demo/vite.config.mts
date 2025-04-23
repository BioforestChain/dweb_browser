import vue from "@vitejs/plugin-vue";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";
import vuetify, { transformAssetUrls } from "vite-plugin-vuetify";
import { toolkitResolverPlugin } from "../../../scripts/vite-npm-resolver-plugin.mts";

export default defineConfig({
  plugins: [
    toolkitResolverPlugin(),
    vue({
      customElement: /^dweb\-/,
      template: {
        transformAssetUrls,
        compilerOptions: {
          isCustomElement: (tag) => tag.startsWith("dweb-"),
        },
      },
    }),
    vuetify({
      autoImport: true,
      styles: true,
    }),
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
      "@plaoc/is-dweb": fileURLToPath(new URL("../../../../npm/@plaoc__is-dweb", import.meta.url)),
      "@plaoc/plugins": fileURLToPath(new URL("../../../../npm/@plaoc__plugins", import.meta.url)),
    },
    extensions: [".js", ".json", ".jsx", ".mjs", ".ts", ".tsx", ".vue"],
  },
  server: {
    port: 4399,
  },
});
