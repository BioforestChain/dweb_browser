import { defineConfig } from "vite";
export default defineConfig((env) => {
  return {
    root: "./src",

    build: {
      rollupOptions: {
        input: {
          index: "./src/index.html",
          worker: "./src/worker.ts",
        },
      },
      target: "es2020",
      minify: true,
      emptyOutDir: true,
      outDir: "../dist",
    },
    worker: {
      format: "es",
    },
  };
});
