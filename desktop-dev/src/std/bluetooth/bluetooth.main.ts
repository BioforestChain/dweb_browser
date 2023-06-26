import { BrowserView } from "electron";
import { ALL_MMID_MWEBVIEW_WINDOW_MAP } from "../../browser/multi-webview/multi-webview.mobile.wapi.ts";
import { Ipc, IpcResponse } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { createComlinkNativeWindow, createComlinkBrowserView } from "../../helper/openNativeWindow.ts";
import { createHttpDwebServer, type HttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import type { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.ts"
import type { $Device } from "./types.ts";
import type { Remote } from "comlink";

type $APIS = typeof import("./assets/exportApis.ts")["APIS"];

export class BluetoothNMM extends NativeMicroModule {
  mmid = "bluetooth.std.dweb" as const;
  _bv: BrowserView | undefined;
  _apis: Remote<$APIS> | undefined;
  _selectBluetoothCallback: { (id: string): void } | undefined;
  _httpDwebServer: HttpDwebServer | undefined;
  _wwwReadableStream: ReadableStreamIpc | undefined;
  _mountedWindow: Electron.BrowserWindow | null = null;
  private _browserView?: ReturnType<BluetoothNMM["_createBrowserView"]> | undefined;
  _rootUrl = ""
 
  _bootstrap = async () => {
    console.always(`[${this.mmid} _bootstrap]`);

    // 创建服务
    this._rootUrl = await this._createHttpDwebServer()
    this._openUI()

    // 先注册处理器
    this.registerCommonIpcOnMessageHandler({
      method: "POST", 
      pathname: "/device_list_update",
      matchMode: "full",
      input: {},
      output: "object",
      handler: async (arg, ipc, request) => {
        if(this._browserView === undefined){
          this._openUI()
        }
        const deviceList: any = JSON.parse(await request.body.text());
        if(this._apis === undefined) throw new Error('this._apis === undefined');
        this._apis.devicesUpdate(deviceList);
        return true;
      }
    })

    /**
     * 关闭
     */
    this.registerCommonIpcOnMessageHandler({
      pathname: "/close",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: async (arg, ipc, request) => {
        this._closeUI();
        const b = await this.nativeFetch(`file://mwebview.browser.dweb/bluetooth/device/selected?id=""`).boolean()
        return true;
      },
    });
  };

  /**
   * 创建服务
   * @returns 
   */
  private _createHttpDwebServer = async () => {
    this._httpDwebServer = await createHttpDwebServer(this, {});
    this._wwwReadableStream = await this._httpDwebServer.listen();
    this._wwwReadableStream.onRequest(async (request, ipc) => {
      const url = "file:///sys/bluetooth" + request.parsed_url.pathname;
      ipc.postMessage(
        await IpcResponse.fromResponse(
          request.req_id,
          await this.nativeFetch(
            "file:///sys/bluetooth" + request.parsed_url.pathname
          ),
          ipc
        )
      );
    });
    
    const rootUrl = this._httpDwebServer.startResult.urlInfo.buildInternalUrl(
      (url) => {
        url.pathname = "/index.html";
      }
    ).href;

    return rootUrl
  }

  /**
   * 创建一个新的隐藏窗口装载webview，使用它的里头 web-bluetooth-api 来实现我们的需求
   * @param url
   * @param ipc
   * @returns
   */
  // private _createBrowserView = async (url: string, ipc?: Ipc) => createComlinkNativeWindow(
  //   url,
  //   {
  //     webPreferences: {
  //       sandbox: false,
  //       devTools: true,
  //       webSecurity: false,
  //       nodeIntegration: true,
  //       contextIsolation: false,
  //     },
  //     show: true,
  //   },
  //   async (win) => {
  //     return {
  //       deviceSelected: async (device: $Device) => {
  //         console.always('接受到了选择 device', device)
  //         const b = await this.nativeFetch(`file://mwebview.browser.dweb/bluetooth/device/selected?id=${device.deviceId}`).boolean()
  //         if(this._apis === undefined) throw new Error('this._apis === undefined');
  //         this._apis.deviceSelected(b ? device : undefined)
  //         // 关闭UI
  //         this._closeUI()
  //       },
  //     };
  //   }
  // );

  private _createBrowserView = async (url: string, ipc?: Ipc) => createComlinkBrowserView(
    url,
    {
      webPreferences: {
        sandbox: false,
        devTools: true,
        webSecurity: false,
        nodeIntegration: true,
        contextIsolation: false,
      },
      show: true,
    },
    async (win) => {
      return {
        deviceSelected: async (device: $Device) => {
          console.always('接受到了选择 device', device)
          const b = await this.nativeFetch(`file://mwebview.browser.dweb/bluetooth/device/selected?id=${device.deviceId}`).boolean()
          if(this._apis === undefined) throw new Error('this._apis === undefined');
          this._apis.deviceSelected(b ? device : undefined)
          // 关闭UI
          this._closeUI()
        },
      };
    }
  );

  // 关闭 UI 
  private _closeUI = async () => {
    if(this._browserView === undefined) throw new Error("this._browserWindow === undefined");
    (await this._browserView)?.webContents.close();
    this._mountedWindow?.removeBrowserView(await this._browserView)
    this._browserView = undefined;
  }

  // 打开 browseView
  private _openUI = async () => {
    this._mountedWindow = Electron.BrowserWindow.getFocusedWindow();
    await (this._browserView = this._getBrowwerView(this._rootUrl));
    if(this._browserView === undefined) throw new Error(`this._browserView === undefined`);
    this._mountedWindow?.addBrowserView(await this._browserView);
    const bounds = this._mountedWindow?.getBounds();
    const contentBounds = this._mountedWindow?.getContentBounds();
    if(contentBounds === undefined) throw new Error(`contentBounds === undefined`);
    if(bounds === undefined) throw new Error(`bounds === undefined`);
    const titleBarHeight = bounds.height - contentBounds.height;
    (await this._browserView).setAutoResize({
      width: true
    })
    ;(await this._browserView).setBounds({
      x: 0,
      y: titleBarHeight,
      width: contentBounds?.width,
      height: 200
    })
    this._apis = (await this._browserView)?.getApis();
  }
  
  private _getBrowwerView = (url: string, ipc?: Ipc) => {
    return (this._browserView ??= this._createBrowserView(url, ipc));
  };

  protected override _shutdown = async () => {
    this._httpDwebServer?.close();
    this._wwwReadableStream?.close();
    this._closeUI()
  }
}