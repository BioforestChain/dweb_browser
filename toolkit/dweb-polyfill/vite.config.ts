import { defineConfig } from "vite";
export default defineConfig(() => {
  return {
    build: {
      target: "esnext",
      lib: {
        entry: {
          "keyboard.android": "./src/keyboard.android.ts",
          "websocket.ios": "./src/websocket.ios.ts",
          "favicon.ios": "./src/favicon.ios.ts",
          "favicon.common": "./src/favicon.common.ts",
          "close-watcher.common": "./src/close-watcher/index.ts",
          "user-agent-data.common": "./src/user-agent-data.common.ts",
        },
        // 多入口不支持 iife
        formats: ["cjs"],
      },
      minify: false,
      emptyOutDir: true,
      outDir: "../../next/kmp/dwebview/src/commonMain/composeResources/files/dwebview-polyfill",
    },
  };
});
