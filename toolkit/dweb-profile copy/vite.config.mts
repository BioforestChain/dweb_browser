import { defineConfig } from "vite";
export default defineConfig(() => {
  return {
    build: {
      target: "esnext",
      lib: {
        entry: {
          indexeddb: "./src/indexeddb.ts",
          localstorage: "./src/localstorage.ts",
        },
        // 多入口不支持 iife
        formats: ["es" as const],
      },
      minify: false,
      emptyOutDir: true,
    },
  };
});
