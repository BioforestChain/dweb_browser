import type { $Electron } from "./multi-webview-content-execute-javascript.ts";

const code = () => {
  (() => {
  })()
}
export default code;

declare namespace globalThis{
  let electron: $Electron;
  let __native_close_watcher_kit__: $__native_close_watcher_kit__;
}

export interface $__native_close_watcher_kit__{
  _tasks: Map<string, (id: string) => void>;
  _watchers: {
    close: {(): void}
  }[]
}




 
