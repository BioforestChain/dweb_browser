import { defineConfig } from "vite";
export default defineConfig(() => {
  return {
    build: {
      target: "esnext",
      lib: {
        entry: {
          "websocket.ios": "./src/websocket.ios.ts",
          "favicon.ios": "./src/favicon.ios.ts",
        },
        formats: ["cjs"],
      },
      minify: false,
      emptyOutDir: true,
      outDir: "../../next/kmp/shared/src/commonMain/resources/dwebview-polyfill",
    },
  };
});
