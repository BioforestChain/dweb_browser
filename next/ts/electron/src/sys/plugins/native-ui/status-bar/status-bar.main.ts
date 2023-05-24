// 模拟状态栏模块-用来提供状态UI的模块
import { NativeMicroModule } from "../../../../core/micro-module.native.js";
import { log } from "../../../../helper/devtools.js"
import { 
  getState, setState, startObserve, stopObserve
 } from "./handlers.js"
import type { Ipc } from "../../../../core/ipc/ipc.js";
import type { IpcRequest } from "../../../../core/ipc/IpcRequest.js";
import type { $BootstrapContext } from "../../../../core/bootstrapContext.js"
import type { IncomingMessage, OutgoingMessage } from "http";
import { IpcEvent } from "../../../../core/ipc/IpcEvent.js";
import { ipcMain, IpcMainEvent } from "electron";
// import type { $Schema1, $Schema2 } from "../../../../helper/types.ts";
export class StatusbarNativeUiNMM extends NativeMicroModule {
  mmid = "status-bar.nativeui.sys.dweb" as const;
  httpIpc: Ipc | undefined
  observes: Map<string /** headers.host */ , Ipc> = new Map();
  observesState: Map<string /**headers.host */, boolean>  = new Map();
  encoder = new TextEncoder();

  _bootstrap = async (context: $BootstrapContext) => {
    log.green(`[${this.mmid} _bootstrap]`)

    {
      // 监听从 multi-webview-comp-status-bar.html.mts 通过 ipcRenderer 发送过来的 监听数据
      ipcMain.on(
        'status_bar_state_change', 
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

      if(event.name === "updated"){

      }
    })
  }

  _shutdown() {
    
  }
}

export interface $Operation {
  acction: string;
  value: string;
}
 
export interface $StatusbarHtmlRequest {
  ipc: Ipc;
  request: IpcRequest;
  appUrl: string; // appUrl 标识 当前statusbar搭配的是哪个 app 显示的
}

export enum $StatusbarStyle {
  light = "light",
  dark = "dark",
  default = "default",
}

export type $isOverlays =
  | "0" // 不覆盖
  | "1"; // 覆盖

export interface $ReqRes{
  req: IncomingMessage, 
  res: OutgoingMessage
}

export interface $Observe{
  res: OutgoingMessage | undefined;
  isObserve: boolean;
}