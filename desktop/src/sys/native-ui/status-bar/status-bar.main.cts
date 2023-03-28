// 模拟状态栏模块-用来提供状态UI的模块
import { NativeMicroModule } from "../../../core/micro-module.native.cjs";
import { intercept } from "../intercept-http.cjs"
import { AllConnects } from "./on-connect-callback.cjs";
import { WWWServer }from "./www-server.cjs";
import { CommonMesasgeRoutes } from "./register-common-ipc-on-message.cjs";
import { PluginsRequest } from "../plugins-request.cjs"
import { log } from "../../../helper/devtools.cjs"
import { IpcEvent } from "../../../core/ipc/IpcEvent.cjs";
import { HttpConnect } from "./http-connect.cjs"
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
  pluginsRequest = new PluginsRequest();
  private _allConnects: AllConnects = new AllConnects()
  private _httpConnect: any;

  _bootstrap = async (context: $BootstrapContext) => {
    log.green(`[${this.mmid} _bootstrap]`)

    {
      this.onConnect(this._allConnects.onConnect)
    }
    
    {
      new HttpConnect(this,context, this.mmid);
    }
   
    {
      new WWWServer(this)
    }
    
    // {
    //   new CommonMesasgeRoutes(this)
    //   .routes
    //   .forEach(
    //     (item: $RequestCommonHanlderSchema<$Schema1, $Schema2>) => {
    //       this.registerCommonIpcOnMessageHandler(item);
    //     }
    //   );
    // }
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

