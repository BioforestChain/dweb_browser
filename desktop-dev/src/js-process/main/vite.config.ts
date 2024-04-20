// import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
  base: "./",
  build: {
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
  // plugins: [vue()],
  // resolve: {
  //   alias: {
  //     "@": fileURLToPath(new URL("./src", import.meta.url)),
  //     "&": fileURLToPath(new URL("../", import.meta.url)),
  //     "helper/": fileURLToPath(new URL("../../helper", import.meta.url)) + "/",
  //   },
  // },
});
