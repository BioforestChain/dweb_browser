import type { Remote } from "comlink";
import type { $DWEB_DEEPLINK } from "../../core/helper/types.ts";
import type { ReadableStreamIpc } from "../../core/ipc-web/index.ts";
import { Ipc, IpcResponse } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { createComlinkNativeWindow } from "../../helper/openNativeWindow.ts";
import {
  createHttpDwebServer,
  type HttpDwebServer,
} from "../../sys/http-server/$createHttpDwebServer.ts";
import type { $Device } from "./types.ts";

type $APIS = typeof import("./assets/exportApis.ts")["APIS"];

export class BluetoothNMM extends NativeMicroModule {
  mmid = "bluetooth.std.dweb" as const;
  dweb_deeplinks = ["dweb:bluetooth"] as $DWEB_DEEPLINK[];
  _apis: Remote<$APIS> | undefined;
  _selectBluetoothCallback: { (id: string): void } | undefined;
  _httpDwebServer: HttpDwebServer | undefined;
  _wwwReadableStreamIpc: ReadableStreamIpc | undefined;
  _mountedWindow: Electron.BrowserWindow | null = null;
  private _browserWindow?: ReturnType<BluetoothNMM["_createBrowserWindow"]>;
  _rootUrl = "";
  private _requestDeviceOptions: RequestDeviceOptions | undefined;
  private _deviceConnectedResolve(value: unknown) {}
  private _deviceDisconnectedResolve(value: unknown) {}

  _bootstrap = async () => {
    console.always(`[${this.mmid} _bootstrap]`);

    // 创建服务
    this._rootUrl = await this._createHttpDwebServer();

    this.registerCommonIpcOnMessageHandler({
      method: "POST",
      pathname: "/open",
      matchMode: "full",
      input: {},
      output: "object",
      handler: async (_, ipc, request) => {
        this._requestDeviceOptions = JSON.parse(await request.body.text());
        if (this._requestDeviceOptions === undefined)
          throw new Error(`this._requestDeviceOptions === undefined`);
        console.always("/open", this._browserWindow);
        if (this._browserWindow === undefined) {
          this._openUI();
        } else {
          this._maximize();
        }

        const device = await new Promise(
          (resolve) => (this._deviceConnectedResolve = resolve)
        );
        return device;
      },
    });

    /**
     * 断开连接
     */
    this.registerCommonIpcOnMessageHandler({
      pathname: "/bluetooth_remote_gatt_server/disconnect",
      matchMode: "full",
      input: { id: "string" },
      output: "object",
      handler: async (args, ipc, request) => {
        // 开发状态下需要 打开UI
        // 非开发状态下是不需要打开UI的
        this._maximize();
        const id = args.id;
        if (this._apis === undefined)
          throw new Error(`this._apis === undefined`);
        this._apis.deviceDisconnect(id);
        // 打开之后前端的 UI 必须要保持住
        const result = await new Promise(
          (resolve) => (this._deviceDisconnectedResolve = resolve)
        );
        return result;
      },
    });

    /**
     * 关闭 蓝牙功能
     */
    this.registerCommonIpcOnMessageHandler({
      pathname: "/close",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: async (arg, ipc, request) => {
        // this._minimize();
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
        this._openUI();
      },
    });
  };

  /**
   * 创建服务
   * @returns
   */
  private _createHttpDwebServer = async () => {
    this._httpDwebServer = await createHttpDwebServer(this, {});
    this._wwwReadableStreamIpc = await this._httpDwebServer.listen();
    this._wwwReadableStreamIpc.onRequest(async (request, ipc) => {
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

    return rootUrl;
  };

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
            console.always("接受到了选择 device", device);
            console.always(
              "this._bluetoothrequestdevicewatchSelectCallback",
              this._bluetoothrequestdevicewatchSelectCallback
            );
            if (this._bluetoothrequestdevicewatchSelectCallback === undefined) {
              this._apis?.deviceSelectedCallback();
              return;
            }
            this._bluetoothrequestdevicewatchSelectCallback(device.deviceId);
            this._bluetoothrequestdevicewatchSelectCallback = undefined;
          },
          // 设备连接成功
          deviceConnectedSuccess: async (server: BluetoothRemoteGATTServer) => {
            console.always("设备连接成功", server);
            this._deviceConnectedResolve(server);
          },
          requestDeviceFail: async () => {
            this._requestDevice();
          },
          // 设备断开连接操作的回调
          deviceDisconnectCallback: async (arg: unknown) => {
            console.always("arg: ", arg);
            this._deviceDisconnectedResolve(arg);
          },
        };
      }
    );
    bw.on("blur", () => {
      this._minimize();
    });
    return bw;
  };

  // 关闭 UI
  private _closeUI = async (bw?: Electron.BrowserWindow) => {
    if (bw === undefined) {
      bw = await this._browserWindow;
    }
    if (bw === undefined) {
      return false;
    }
    bw.close();
    this._browserWindow = undefined;
  };

  // 最小化 UI
  private _minimize = async () => {
    if (this._browserWindow === undefined) {
      throw new Error("this._browserWindow === undefined");
    }
    (await this._browserWindow).minimize();
  };

  private _maximize = async () => {
    if (this._browserWindow === undefined) {
      throw new Error("this._browserWindow === undefined");
    }
    (await this._browserWindow).maximize();
  };

  // 打开 browseView
  private _openUI = async () => {
    this._browserWindow = this._getBrowserWindow(this._rootUrl);
    const bw = await this._browserWindow;
    const apis = bw.getApis<$APIS>();
    this._apis = apis;
    this._bluetoothrequestdevicewatch(bw, apis);
    this._requestDevice();
  };

  private _getBrowserWindow = (url: string, ipc?: Ipc) => {
    return (this._browserWindow ??= this._createBrowserWindow(url, ipc));
  };

  private _requestDevice = async () => {
    if (this._browserWindow === undefined)
      throw new Error(`this._browserWindow === undefined`);
    (await this._browserWindow).webContents.executeJavaScript(
      `requestDevice(${JSON.stringify(this._requestDeviceOptions)})`,
      true
    );
  };

  private _bluetoothrequestdevicewatchSelectCallback:
    | { (deviceId: string): void }
    | undefined;
  private _bluetoothrequestdevicewatch = async (
    bw: Electron.BrowserWindow,
    apis: Remote<$APIS>
  ) => {
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
        apis.devicesUpdate(deviceList);
      }
    );
  };

  protected override _shutdown = async () => {
    this._httpDwebServer?.close();
    this._wwwReadableStreamIpc?.close();
    this._closeUI();
  };
}

// "TypeError: Invalid state: Controller is already closed\n
// at new NodeError (node:internal/errors:399:5)\n
// at ReadableStreamDefaultController.enqueue (node:internal/webstreams/readablestream:1036:13)\n
// at /Users/pengxiaohua/project/dweb_browser/desktop-dev/src/helper/readableStreamHelper.ts:164:20\n
// at Signal.value (/Users/pengxiaohua/project/dweb_browser/desktop-dev/src/helper/createSignal.ts:51:10)\n
// at Signal.value [as emit] (/Users/pengxiaohua/project/dweb_browser/desktop-dev/src/helper/createSignal.ts:44:12)\n
//  at Object.pull (/Users/pengxiaohua/project/dweb_browser/desktop-dev/src/helper/readableStreamHelper.ts:133:33)\n
//  at ensureIsPromise (node:internal/webstreams/util:192:19)\n
//  at readableStreamDefaultControllerCallPullIfNeeded (node:internal/webstreams/readablestream:2254:5)\n
//   at readableStreamDefaultControllerPullSteps (node:internal/webstreams/readablestream:2303:7)\n
//   at ReadableStreamDefaultController.[kPull] (node:internal/webstreams/readablestream:1052:5)"
