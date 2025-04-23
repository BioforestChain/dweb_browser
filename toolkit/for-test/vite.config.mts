import { defineConfig } from "vite";
import { toolkitResolverPlugin } from "../scripts/vite-npm-resolver-plugin.mts";

export default defineConfig({
  plugins: [toolkitResolverPlugin()],
  base: "./",
  server: {
    port: 12207,
  },
});
