import glob from "glob";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [],
  build: {
    rollupOptions: {
      input: Object.fromEntries(
        glob
          .sync("./server/*.ts")
          .map((file: string) => [
            path.relative("./", file.slice(0, file.length - path.extname(file).length)),
            fileURLToPath(new URL(file, import.meta.url)),
          ])
      ),
      output: {
        dir: "plaoc-dist",
        entryFileNames: `[name].js`,
      },
    },
  },
});
