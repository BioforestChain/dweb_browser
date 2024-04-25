import glob from "glob";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [],
  build: {
    outDir: "plaoc-dist",
    lib: {
      entry: Object.fromEntries(
        glob
          .sync("./server/*.ts")
          .map((file: string) => [
            path.relative("./", file.slice(0, file.length - path.extname(file).length)),
            fileURLToPath(new URL(file, import.meta.url)),
          ])
      ),
      formats: ["es"],
      fileName: (_, name) => name + ".js",
    },
  },
});
