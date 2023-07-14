import path from "node:path";
import { defineConfig } from "vite";

// Plugins
import vue from "@vitejs/plugin-vue";
import renderer from "vite-plugin-electron-renderer";
import vuetify, { transformAssetUrls } from "vite-plugin-vuetify";

export default defineConfig({
  base: "./",
  build: {
    rollupOptions: {
      input: {
        taskbar: path.join(__dirname, "/taskbar/index.html"),
      },
    },
  },
  plugins: [
    renderer(),
    vue({
      template: { transformAssetUrls },
    }),
    vuetify({
      autoImport: true,
      styles: {
        configFile: "styles/settings.scss",
      },
    }),
  ],
  resolve: {
    extensions: [".js", ".json", ".jsx", ".mjs", ".ts", ".tsx", ".vue"],
  },
  define: { "process.env": {} },
  server: {
    port: 3700,
  },
});
