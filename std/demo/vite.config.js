import { defineConfig } from 'vite'
export default defineConfig({
  // ...
  root: "./",
  build:{
    minify: false,
    outDir: "./dist",
    emptyOutDir: true,
  }
})