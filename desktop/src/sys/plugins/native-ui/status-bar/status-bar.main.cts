// 模拟状态栏模块-用来提供状态UI的模块
import { NativeMicroModule } from "../../../../core/micro-module.native.cjs";
import { WWWServer }from "./www-server.cjs";
import { log } from "../../../../helper/devtools.cjs"
import querystring from "node:querystring"
import { converRGBAToHexa } from "../../helper.cjs"
import type { Ipc } from "../../../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../../../core/ipc/IpcRequest.cjs";
import type { $BootstrapContext } from "../../../../core/bootstrapContext.cjs"
import type { HttpServerNMM } from "../../../http-server/http-server.cjs";
import type { IncomingMessage, OutgoingMessage } from "node:http";
export class StatusbarNativeUiNMM extends NativeMicroModule {
  mmid = "status-bar.nativeui.sys.dweb" as const;
  httpIpc: Ipc | undefined
  // httpNMM: HttpServerNMM | undefined;
  observe: Map<string, OutgoingMessage> = new Map();
  waitForOperationRes: Map<string, OutgoingMessage> = new Map();
  reqResMap: Map<number, $ReqRes> = new Map();
  observeMap: Map<string, $Observe> = new Map() 
  encoder = new TextEncoder();
  allocId = 0;

  _bootstrap = async (context: $BootstrapContext) => {
    log.green(`[${this.mmid} _bootstrap]`)
    
    //   获取全部的 appsInfo
    this.registerCommonIpcOnMessageHandler({
      pathname: "/getState",
      matchMode: "full",
      input: {},
      output: "object",
      handler: async (args,client_ipc, request) => {
       console.error(`${this.mmid} getState`)
      }
    });
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