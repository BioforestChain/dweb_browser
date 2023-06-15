import type { $Electron } from "./multi-webview-content-execute-javascript.ts";

declare global {
  const electron: $Electron;
  const __native_close_watcher_kit__: $__native_close_watcher_kit__;
}

export interface $__native_close_watcher_kit__ {
  _tasks: Map<string, (id: string) => void>;
  _watchers: {
    close: { (): void };
  }[];
}
