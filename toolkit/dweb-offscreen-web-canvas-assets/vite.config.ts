import { defineConfig } from "vite";
export default defineConfig({
  root: "./src",

  build: {
    target: "es2020",
    minify: true,
    emptyOutDir: true,
    outDir: "../dist",
  },
  worker: {
    format: "es",
  },
});
