import path from "node:path";
import { defineConfig } from "vite";

// Plugins
import renderer from "vite-plugin-electron-renderer";

export default defineConfig({
  base: "./",
  build: {
    rollupOptions: {
      input: {
        taskbar: path.join(__dirname, "index.html"),
      },
    },
  },
  plugins: [renderer()],
  resolve: {
    extensions: [".js", ".json", ".jsx", ".mjs", ".ts", ".tsx", ".vue"],
  },
  define: { "process.env": {} },
  server: {
    port: 3700,
  },
});
