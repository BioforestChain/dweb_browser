// import vue from "@vitejs/plugin-vue";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";
import { toolkitResolverPlugin } from "../../scripts/vite-npm-resolver-plugin.mts";
// https://vitejs.dev/config/
export default defineConfig({
  base: "./",
  build: {
    emptyOutDir: true,
    assetsDir: "./", // 资源目录,相对于 dist 目录
    assetsInlineLimit: 4096, // 小于该大小的资源将内联为 base64 编码
    rollupOptions: {
      output: {
        assetFileNames: `assets/[hash].[ext]`,
        chunkFileNames: `[hash].js`,
      },
    },
  },
  server: {
    port: 5174,
  },
  plugins: [toolkitResolverPlugin()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
      "@dweb-browser/core": fileURLToPath(new URL("../../dweb-core/src", import.meta.url)),
      "@dweb-browser/helper": fileURLToPath(new URL("../../dweb-helper/src", import.meta.url)),
    },
    extensions: [".js", ".json", ".jsx", ".mjs", ".ts", ".tsx", ".vue"],
  },
});
