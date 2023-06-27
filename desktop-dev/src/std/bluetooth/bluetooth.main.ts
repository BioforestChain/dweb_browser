import { BrowserView } from "electron";
import { Ipc, IpcResponse } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { createComlinkNativeWindow } from "../../helper/openNativeWindow.ts";
import { createHttpDwebServer, type HttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import type { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.ts"
import type { $Device } from "./types.ts";
import type { Remote } from "comlink";
import type { $DWEB_DEEPLINK, $MMID } from "../../helper/types.ts";

type $APIS = typeof import("./assets/exportApis.ts")["APIS"];

export class BluetoothNMM extends NativeMicroModule {
  mmid = "bluetooth.std.dweb" as const;
  dweb_deeplinks = ["dweb:bluetooth"] as $DWEB_DEEPLINK[];
  _bv: BrowserView | undefined;
  _apis: Remote<$APIS> | undefined;
  _selectBluetoothCallback: { (id: string): void } | undefined;
  _httpDwebServer: HttpDwebServer | undefined;
  _wwwReadableStream: ReadableStreamIpc | undefined;
  _mountedWindow: Electron.BrowserWindow | null = null;
  private _browserWindow?: ReturnType<BluetoothNMM["_createBrowserWindow"]>;
  _rootUrl = ""
 
  _bootstrap = async () => {
    console.always(`[${this.mmid} _bootstrap]`);

    // 创建服务
    this._rootUrl = await this._createHttpDwebServer()

    this.registerCommonIpcOnMessageHandler({
      pathname: "/open",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: async () => {
        this._openUI()
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
        // if(this._bluetoothrequestdevicewatchSelectCallback !== undefined){
        //   this._bluetoothrequestdevicewatchSelectCallback("");
        //   this._bluetoothrequestdevicewatchSelectCallback = undefined;
        // }
        this._closeUI();
        return true;
      },
    });

    /// dweb deeplink
    this.registerCommonIpcOnMessageHandler({
      protocol: "dweb:",
      pathname: "bluetooth",
      matchMode: "full",
      input: {},
      output: "void",
      handler: async (args) => {
        console.always("bluetooth");
        this._openUI()
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
  private _createBrowserWindow = async (url: string, ipc?: Ipc) => {
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
        show: true,
      },
      async (win) => {
        return {
          deviceSelected: async (device: $Device) => {
            console.always('接受到了选择 device', device)
            if(this._bluetoothrequestdevicewatchSelectCallback === undefined){
              throw new Error(`this._bluetoothrequestdevicewatchSelectCallback === undefined`);
            }
            this._bluetoothrequestdevicewatchSelectCallback(device.deviceId);
            if(this._browserWindow === undefined) throw new Error(`this._browserWindow === undefined`);
            (await this._browserWindow).webContents.executeJavaScript(
              `requestDevice()`,
              true
            )
            // 关闭UI
            // this._closeUI()
          },
          requestDeviceFail: async () => {
            console.always('requestDeviceFail device', )
            if(this._browserWindow === undefined) throw new Error(`this._browserWindow === undefined`);
            (await this._browserWindow).webContents.executeJavaScript(
              `requestDevice()`,
              true
            )
          }
        };
      }
    ); 
    bw.on('blur', () => {
      this._closeUI();
    })
    return bw; 
  }

  // 关闭 UI 
  private _closeUI = async () => {
    if(this._browserWindow === undefined) throw new Error("this._browserWindow === undefined");
    (await this._browserWindow).close();
    this._browserWindow = undefined;
  }

  // 打开 browseView
  private _openUI = async () => {
    this._browserWindow = this._getBrowserWindow(this._rootUrl);
    const bw = await this._browserWindow;
    this._apis = bw?.getApis();
    this._bluetoothrequestdevicewatch(bw);
    (await this._browserWindow).webContents.executeJavaScript(
      `requestDevice()`,
      true
    )
  }
  
  private _getBrowserWindow = (url: string, ipc?: Ipc) => {
    return (this._browserWindow ??= this._createBrowserWindow(url, ipc));
  };

  private _bluetoothrequestdevicewatchSelectCallback: {(deviceId: string): void} | undefined;
  private _bluetoothrequestdevicewatch = async (bw: Electron.BrowserWindow) => {
    bw.webContents.on(
      "select-bluetooth-device",
      async (
        event: Event,
        deviceList: any[],
        callback: { (id: string): void }
      ) => {
        console.always("select-bluetooth-device; ", Date.now());
        event.preventDefault();
        this._bluetoothrequestdevicewatchSelectCallback = callback;
        this._apis?.devicesUpdate(deviceList);
      }
    );
  }

  protected override _shutdown = async () => {
    this._httpDwebServer?.close();
    this._wwwReadableStream?.close();
    this._closeUI()
  }
}