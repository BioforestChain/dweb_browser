// 模拟状态栏模块-用来提供状态UI的模块
import type { Remote } from "comlink";
import { match } from "ts-pattern";
import { MICRO_MODULE_CATEGORY } from "../../core/category.const.ts";
import type { $OnFetch, FetchEvent } from "../../core/helper/ipcFetchHelper.ts";
import { IPC_METHOD } from "../../core/ipc/const.ts";
import { IpcHeaders, IpcResponse } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { createComlinkNativeWindow } from "../../helper/openNativeWindow.ts";
import { createHttpDwebServer, type HttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
type $APIS = typeof import("./assets/exportApis.ts")["APIS"];

export class BarcodeScanningNMM extends NativeMicroModule {
  mmid = "barcode-scanning.sys.dweb" as const;
  name = "Barcode Scanning";
  override categories = [MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Utilities];
  private _allocId = 0;
  private _operationResolveMap = new Map<number, { (arg: any): void }>();
  private _responseHeader = new IpcHeaders().init("Content-Type", "application/json");

  // 暴露给 client 使用的方法
  private _exports = {
    operationCallback: async (arg: unknown, resolveId: number) => {
      const resolve = this._operationResolveMap.get(resolveId);
      if (resolve === undefined) {
        throw new Error(`this._operationResolveMap.get(${resolveId})`);
      }
      resolve(arg);
      this._operationResolveMap.delete(resolveId);
    },
  };
  private _browserWindow: undefined | Awaited<ReturnType<typeof this._createBrowserWindow>>;

  // 以某个函数返回的值作为类型
  // private _browserWindow: ReturnType<typeof createComlinkNativeWindow> | undefined;
  private _apis: Remote<$APIS> | undefined;
  private _rootUrl = "";
  private _httpDwebServer: HttpDwebServer | undefined;
  _bootstrap = async () => {
    await this._createHttpDwebServer();
    this.onFetch(async (event: FetchEvent) => {
      return match(event)
        .with({ method: IPC_METHOD.POST, pathname: "/process" }, async () => {
          return this._processHandler(event);
        })
        .with({ method: IPC_METHOD.GET, pathname: "/stop" }, async () => {
          return this._stopHandler(event);
        })
        .with({ method: IPC_METHOD.GET, pathname: "/get_supported_formats" }, async () => {
          return this._getSupportedFormats(event);
        })
        .run();
    }).internalServerError();
  };

  _shutdown() {
    this._httpDwebServer?.close();
    if (this._browserWindow) {
      this._closeUI(this._browserWindow);
    }
  }

  // 关闭 UI
  private _closeUI = async (bw: Electron.BrowserWindow) => {
    bw.close();
    this._browserWindow = undefined;
    this._apis = undefined;
  };

  private _processHandler: $OnFetch = async (event: FetchEvent) => {
    if (this._browserWindow === undefined) {
      await this._initUI();
    }

    const uint8Array = await event.ipcRequest.body.u8a();
    const resolveId = this._allocId++;
    const resPromise = new Promise((resolve) => this._operationResolveMap.set(resolveId, resolve));
    this._apis?.process(uint8Array, resolveId);
    const res = await resPromise;
    return IpcResponse.fromJson(event.ipcRequest.req_id, 200, this._responseHeader, res, event.ipc);
  };

  private _stopHandler: $OnFetch = async (event: FetchEvent) => {
    if (this._browserWindow === undefined) {
      await this._initUI();
    }
    const resolveId = this._allocId++;
    const resPromise = new Promise((resolve) => this._operationResolveMap.set(resolveId, resolve));
    this._apis?.stop(resolveId);
    const res = await resPromise;
    return IpcResponse.fromJson(event.ipcRequest.req_id, 200, this._responseHeader, res, event.ipc);
  };
  private _getSupportedFormats: $OnFetch = async (event: FetchEvent) => {
    if (this._browserWindow === undefined) {
      await this._initUI();
    }
    const resolveId = this._allocId++;
    const resPromise = new Promise((resolve) => this._operationResolveMap.set(resolveId, resolve));
    this._apis?.getSupportedFormats(resolveId);
    const res = await resPromise;
    return IpcResponse.fromJson(event.ipcRequest.req_id, 200, this._responseHeader, res, event.ipc);
  };

  protected _initUI = async () => {
    const bw = (this._browserWindow = await this._createBrowserWindow(this._rootUrl));
    this._apis = await bw.getRenderApi<$APIS>();
    return {
      bw: bw,
      apis: this._apis,
    };
  };

  _createBrowserWindow = async (url: string) => {
    const bw = await createComlinkNativeWindow(
      url,
      {
        webPreferences: {
          sandbox: false,
          devTools: true,
          webSecurity: false,
          nodeIntegration: true,
          contextIsolation: false,
        },
        show: false,
      },
      async (win) => this._exports
    );
    return bw;
  };

  private _createHttpDwebServer = async () => {
    this._httpDwebServer = await createHttpDwebServer(this, {});
    const serverIpc = await this._httpDwebServer.listen();
    serverIpc.onFetch(async (event: FetchEvent) => {
      return await this.nativeFetch("file:///sys/barcode-scanning" + event.pathname);
    });
    this._rootUrl = this._httpDwebServer.startResult.urlInfo.buildInternalUrl((url: URL) => {
      url.pathname = "/index.html";
    }).href;
    return this;
  };
}
