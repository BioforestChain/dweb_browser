// Plugins

// Utilities
import path from "node:path";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
  build: {
    rollupOptions: {
      input: {
        index: path.resolve(__dirname, "index.html"),
        zh: path.join("zh", "/index.html"),
        en: path.join("en", "/index.html"),
      },
    },
    minify: false,
  },
  plugins: [],
  define: { "process.env": {} },
  server: {
    port: 3600,
  },
});
