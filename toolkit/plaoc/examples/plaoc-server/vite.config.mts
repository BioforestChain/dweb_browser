import { glob } from "glob";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";
import { toolkitResolverPlugin } from "../../../scripts/vite-npm-resolver-plugin.mts";

export default defineConfig({
  plugins: [toolkitResolverPlugin()],
  build: {
    target: "esnext",
    lib: {
      entry: Object.fromEntries(
        glob
          .sync("./middlewares/*.ts")
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
