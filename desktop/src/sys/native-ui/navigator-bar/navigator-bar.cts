// 模拟状态栏模块-用来提供状态UI的模块
import fsPromises from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { IpcHeaders } from "../../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../../core/ipc/IpcResponse.cjs";
import { NativeMicroModule } from "../../../core/micro-module.native.cjs";
import { createHttpDwebServer } from "../../http-server/$createHttpDwebServer.cjs";

import type { Remote } from "comlink";
import type { Ipc } from "../../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs";
import type { $NativeWindow } from "../../../helper/openNativeWindow.cjs";
import { IpcEvent } from "../../../core/ipc/IpcEvent.cjs";
import chalk from "chalk";
import type { $BootstrapContext } from "../../../core/bootstrapContext.cjs"
import { log } from "../../../helper/devtools.cjs"

 
// @ts-ignore
type $APIS = typeof import("./assets/multi-webview.html.mjs")["APIS"];
export class NavigatorBarNativeUiNMM extends NativeMicroModule {
  mmid = "navigation-bar.nativeui.sys.dweb" as const;
  private _uid_wapis_map = new Map<
    number,
    { nww: $NativeWindow; apis: Remote<$APIS> }
  >();

  _bootstrap = async (context: any) => {

  }

  _shutdown = async () => {

  }

}


