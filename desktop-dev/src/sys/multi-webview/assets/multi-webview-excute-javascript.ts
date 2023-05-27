import type { $Electron } from "./multi-webview-content-execute-javascript.ts";
const code = () => {
  (() => {
    const watchers = Array.from(window.__native_close_watcher_kit__._watchers.values())
    if(watchers.length === 0){
      console.log('4')
      if(history.state === null || history.state.back === null){
        window.electron.ipcRenderer.sendToHost(
          'webveiw_message',
          "back"
        )
      }else{
        console.log('3')
        ;(window as unknown as Window).history.back();
      }
    }else{
      console.log('2')
      watchers[watchers.length - 1].close()
    }
  })()
}
export default code;

declare namespace window{
  let electron: $Electron;
  let __native_close_watcher_kit__: $__native_close_watcher_kit__;
}

export interface $__native_close_watcher_kit__{
  _watchers: {
    close: {(): void}
  }[]
}




 
