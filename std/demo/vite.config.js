import { defineConfig } from 'vite'
export default defineConfig({
  // ...
  root: "./",
  build:{
    minify: false,
    outDir: "../../desktop-dev/electron/apps/app.std.dweb/usr/www",
    emptyOutDir: true,
  }
})