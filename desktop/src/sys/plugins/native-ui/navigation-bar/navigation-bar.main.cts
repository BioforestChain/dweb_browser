// 模拟状态栏模块-用来提供状态UI的模块
import { NativeMicroModule } from "../../../../core/micro-module.native.cjs";
import { log } from "../../../../helper/devtools.cjs"
import { 
  getState, setState, startObserve, stopObserve
 } from "./handlers.cjs"
import { IpcEvent } from "../../../../core/ipc/IpcEvent.cjs";
import { ipcMain, IpcMainEvent } from "electron/main";
import type { Ipc } from "../../../../core/ipc/ipc.cjs";

export class NavigationBarNMM extends NativeMicroModule {
  mmid = "navigation-bar.nativeui.sys.dweb" as const;
  httpIpc: Ipc | undefined
  observes: Map<string /** headers.host */ , Ipc> = new Map();
  observesState: Map<string /**headers.host */, boolean>  = new Map();
  encoder = new TextEncoder()

  _bootstrap = async (context: any) => {
    log.green(`[${this.mmid} _bootstrap]`)

    {
      // 监听从 multi-webview-comp-status-bar.html.mts 通过 ipcRenderer 发送过来的 监听数据
      ipcMain.on(
        'navigation_bar_state_change', 
        (ipcMainEvent: IpcMainEvent, host, statusbarState) => {
          const b = this.observesState.get(host)
          if(b === true){
            const ipc = this.observes.get(host);
            if(ipc === undefined) throw new Error(`ipc === undefined`);
            ipc.postMessage(
              IpcEvent.fromText(
                "observe",
                `${JSON.stringify(statusbarState)}`
              )
            )
          }
        }
      )
    }

    this.registerCommonIpcOnMessageHandler({
      pathname: "/getState",
      matchMode: "full",
      input: {},
      output: "object",
      handler: getState.bind(this)
    });    
    
    this.registerCommonIpcOnMessageHandler({
      pathname: "/setState",
      matchMode: "full",
      input: {},
      output: "object",
      handler: setState.bind(this)
    });
     
    this.registerCommonIpcOnMessageHandler({
      pathname: "/startObserve",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: startObserve.bind(this)
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/stopObserve",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: stopObserve.bind(this)
    });

  }

  override _onConnect(ipc: Ipc){
    ipc.onEvent((event: IpcEvent) => {
      if(event.name === "observe"){
        const host = event.data;
        this.observes.set(host as string, ipc)
      }
    })
  }

  _shutdown = async () => {

  }

}


