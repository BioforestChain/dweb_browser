// Plugins
import vue from "@vitejs/plugin-vue";
import vuetify, { transformAssetUrls } from "vite-plugin-vuetify";
import { toolkitResolverPlugin } from "../scripts/vite-npm-resolver-plugin.mts";

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
        desktop: path.join(__dirname, "/desktop.html"),
        taskbar: path.join(__dirname, "/taskbar.html"),
        setting: path.join(__dirname, "/setting.html"),
        address: path.join(__dirname, "/address.html"),
        error: path.join(__dirname, "/error.html"),
      },
      output: {
        assetFileNames: `assets/F[name]-[hash].[ext]`,
        chunkFileNames: `F[name]-[hash].js`,
      },
    },
  },
  plugins: [
    toolkitResolverPlugin(),
    // renderer(),
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
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
      "@dweb-browser/core": fileURLToPath(new URL("../dweb-core/src", import.meta.url)),
      "@dweb-browser/helper": fileURLToPath(new URL("../dweb-helper/src", import.meta.url)),
      "@dweb-browser/polyfill": fileURLToPath(new URL("../dweb-polyfill/src", import.meta.url)),
    },
  },
  server: {
    port: 3600,
  },
});
