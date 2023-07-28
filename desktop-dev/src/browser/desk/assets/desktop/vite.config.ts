// Plugins
import vue from "@vitejs/plugin-vue";
import renderer from "vite-plugin-electron-renderer";
import vuetify, { transformAssetUrls } from "vite-plugin-vuetify";

// Utilities
import path from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
  base: "./",
  build: {
    rollupOptions: {
      input: {
        index: path.join(__dirname, "/index.html"),
        setting: path.join(__dirname, "/setting.html"),
        address: path.join(__dirname, "/address.html"),
        error: path.join(__dirname, "/error.html"),
      },
    },
  },
  plugins: [
    renderer(),
    vue({
      template: { transformAssetUrls },
    }),
    // https://github.com/vuetifyjs/vuetify-loader/tree/next/packages/vite-plugin
    vuetify({
      autoImport: true,
      styles: {
        configFile: "src/styles/settings.scss",
      },
    }),
  ],
  define: { "process.env": {} },
  resolve: {
    alias: [
      {
        find: "src/",
        replacement: fileURLToPath(new URL("./src", import.meta.url)) + "/",
      },
      {
        find: "helper/",
        replacement: fileURLToPath(new URL("../../../../helper", import.meta.url)) + "/",
      },
    ],
    extensions: [".js", ".json", ".jsx", ".mjs", ".ts", ".tsx", ".vue"],
  },
  server: {
    port: 3600,
  },
});
