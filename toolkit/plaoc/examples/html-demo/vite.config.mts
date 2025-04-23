// Plugins
import { toolkitResolverPlugin } from "../../../scripts/vite-npm-resolver-plugin.mts";

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
  plugins: [toolkitResolverPlugin()],
  define: { "process.env": {} },
  server: {
    port: 3600,
  },
});
