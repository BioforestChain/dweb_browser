// 模拟状态栏模块-用来提供状态UI的模块
import { NativeMicroModule } from "../../../core/micro-module.native.cjs";
import { intercept } from "./intercept-http.cjs"
import { AllConnects } from "./on-connect-callback.cjs";
import { WWWServer }from "./www-server.cjs";
import { CommonMesasgeRoutes } from "./register-common-ipc-on-message.cjs";
import { log } from "../../../helper/devtools.cjs"
import type { IpcResponse } from "../../../core/ipc/IpcResponse.cjs"
import type { $Schema1, $Schema2 } from "../../../helper/types.cjs" 
import type {  $RequestCommonHanlderSchema } from "../../../core/micro-module.native.cjs"
import type { Remote } from "comlink";
import type { Ipc } from "../../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs";
import type { $NativeWindow } from "../../../helper/openNativeWindow.cjs";
import type { $BootstrapContext } from "../../../core/bootstrapContext.cjs"

 
// @ts-ignore
type $APIS = typeof import("./assets/multi-webview.html.mjs")["APIS"];
export class StatusbarNativeUiNMM extends NativeMicroModule {
  mmid = "status-bar.nativeui.sys.dweb" as const;
  httpIpc: Ipc | undefined
 
  private _uid_wapis_map = new Map<
    number,
    { nww: $NativeWindow; apis: Remote<$APIS> }
  >();

  // statusbar.plugins发起更改状态栏设置的请求队列
  private _statusbarPluginsRequestMap = new Map<
    string,
    $StatusbarPluginsRequestQueueItem[]
  >(); 
  private _allocId = 0;
  private _allConnects = new AllConnects(this)
  private _wwwServer: WWWServer | undefined;
  
  async _bootstrap(context: $BootstrapContext) {
    log.green(`[${this.mmid} _bootstrap]`)
    const [httpIpc] = await context.dns.connect('http.sys.dweb')
    // 向 httpIpc 发起初始化消息
    intercept(httpIpc, this.mmid)
    this.httpIpc = httpIpc
    this.onConnect(this._allConnects.onConnect)
    this._wwwServer = new WWWServer(this, this._statusbarPluginsRequestMap)
    
    new CommonMesasgeRoutes(this)
      .routes
      .forEach(
        (item: $RequestCommonHanlderSchema<$Schema1, $Schema2>) => {
          this.registerCommonIpcOnMessageHandler(item);
        }
      );
  }

  _statusbarPluginRequestAdd = 
    async (
      app_url: string, 
      request: IpcRequest, 
      callback: {(id: string): void}
    ) => {
      let statusbarPluginRequest =
            this._statusbarPluginsRequestMap.get(app_url);
      const id = `${this._allocId++}`
      const result = await new Promise<IpcResponse>((resolve) => {
        if (statusbarPluginRequest === undefined) {
          statusbarPluginRequest = [];
          this._statusbarPluginsRequestMap.set(
            app_url,
            statusbarPluginRequest
          );
        }
        statusbarPluginRequest.push({
          body: request.body.raw as ReadableStream<Uint8Array>,
          callback: (reponse: IpcResponse) => {
            resolve(reponse);
          },
          req_id: request.req_id,
          id: id,
        });
        callback(id)
      });
      return result;
    } 

  _shutdown() {
    this._uid_wapis_map.forEach((wapi) => {
      wapi.nww.close();
    });
    this._uid_wapis_map.clear();
  }
}

export interface $Operation {
  acction: string;
  value: string;
}

export type $StatusbarPluginsRequestMap 
  = Map<string, $StatusbarPluginsRequestQueueItem[]>

export interface $StatusbarPluginsRequestQueueItem {
  body: ReadableStream<Uint8Array>;
  callback: { (response: IpcResponse): void };
  req_id: number;
  id: string; // 队列项的标识符
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

