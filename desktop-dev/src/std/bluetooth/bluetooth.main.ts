import type { Remote } from "comlink";
import type { ReadableStreamIpc } from "../../core/ipc-web/index.ts";
import { Ipc, IpcResponse } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import type { $DWEB_DEEPLINK } from "../../core/types.ts";
import { createComlinkNativeWindow } from "../../helper/openNativeWindow.ts";
import {
  createHttpDwebServer,
  type HttpDwebServer,
} from "../../std/http/helper/$createHttpDwebServer.ts";
import type { $Device } from "./types.ts";

type $APIS = typeof import("./assets/exportApis.ts")["APIS"];

// UI 的状态
export enum UI_STATUS {
  // 关闭状态
  CLOSED = -1,
  // 已经打开 但是不可见
  HIDE = 0,
  // 已经打开 可以看见能够点击
  VISIBLE = 1,
}

export class BluetoothNMM extends NativeMicroModule {
  mmid = "bluetooth.std.dweb" as const;
  dweb_deeplinks = ["dweb:bluetooth"] as $DWEB_DEEPLINK[];
  private _apis: Remote<$APIS> | undefined;
  private _httpDwebServer: HttpDwebServer | undefined;
  private _wwwReadableStreamIpc: ReadableStreamIpc | undefined;
  private _browserWindow?: ReturnType<BluetoothNMM["_createBrowserWindow"]>;
  private _rootUrl = "";
  private _requestDeviceOptions: RequestDeviceOptions | undefined;
  private _allocId = 0;
  private _operationResolveMap: Map<number, { (arg: unknown): void }> =
    new Map();
  private _deviceConnectedResolve(value: unknown) {}
  // private _deviceDisconnectedResolve(value: unknown) {}
  private _deviceDisconnectedResolveMap: Map<number, { (arg: unknown): void }> =
    new Map();
  private _bluetoothRemoteGATTServerConnectResolveMap: Map<
    number,
    { (arg: unknown): void }
  > = new Map();
  private _deviceGetPrimaryServiceResolveMap: Map<
    number,
    { (arg: unknown): void }
  > = new Map();
  private _deviceGetCharacteristicResolveMap: Map<
    number,
    { (arg: unknown): void }
  > = new Map();
  private _deviceCharacteristicReadValueResolveMap: Map<
    number,
    { (arg: unknown): void }
  > = new Map();
  private _characteristicGetDescriptorMap: Map<
    number,
    { (arg: unknown): void }
  > = new Map();
  private _descriptorReadValueMap: Map<number, { (arg: unknown): void }> =
    new Map();
  private _bluetoothrequestdevicewatchSelectCallback:
    | { (deviceId: string): void }
    | undefined;

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
        if (this._requestDeviceOptions === undefined) {
          return {
            success: false,
            error: `this._requestDeviceOptions === undefined`,
            data: undefined,
          };
        }

        if (this._browserWindow === undefined) {
          this._openUI();
        }
        // else {
        //   this._maximize();
        // }

        const res = await new Promise(
          (resolve) => (this._deviceConnectedResolve = resolve)
        );
        return res;
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
        console.always("可能出现关闭状态下的调用");
        // ui 在关闭状态下
        if (this._browserWindow === undefined) {
          return { success: false, error: "没有开启蓝牙设备" };
        }
        if (this._apis === undefined) {
          console.error("error", `this._apis === undefined`);
          return { success: false, error: "this._apis === undefined" };
        }

        // this._maximize();
        const id = args.id;
        const resolveId = this._allocId++;
        this._apis.deviceDisconnect(id, resolveId);
        // 打开之后前端的 UI 必须要保持住
        const result = await new Promise((resolve) => {
          this._deviceDisconnectedResolveMap.set(resolveId, resolve);
        });
        console.always("disconnect result", result);
        return result;
      },
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/bluetooth_remote_gatt_server/connect",
      matchMode: "full",
      input: { uuid: "string" },
      output: "object",
      handler: async (args) => {
        if (this._browserWindow === undefined) {
          return { success: false, error: "没有开启蓝牙设备" };
        }
        if (this._apis === undefined) {
          console.error("error", `this._apis === undefined`);
          return { success: false, error: "this._apis === undefined" };
        }
        const uuid = args.uuid;
        const resolveId = this._allocId++;
        this._apis.bluetoothRemoteGATTServerConnect(uuid, resolveId);
        const result = new Promise((resolve) =>
          this._operationResolveMap.set(resolveId, resolve)
        );
        return result;
      },
    });

    /**
     * 查询 service
     */
    this.registerCommonIpcOnMessageHandler({
      pathname: "/bluetooth_remote_gatt_server/get_primary_service",
      matchMode: "full",
      input: { uuid: "string" },
      output: "object",
      handler: async (args, ipc, request) => {
        console.always(
          "bluetooth_remote_gatt_server/get_primary_service 接受到了请求"
        );
        if (this._browserWindow === undefined) {
          return { success: false, error: "没有开启蓝牙设备" };
        }
        if (this._apis === undefined) {
          console.error("error", `this._apis === undefined`);
          return { success: false, error: "this._apis === undefined" };
        }

        const uuid = args.uuid;
        const resolveId = this._allocId++;
        // this._maximize();
        this._apis.getPrimarySevice(uuid, resolveId);
        // 打开之后前端的 UI 必须要保持住
        const result = await new Promise((resolve) => {
          this._deviceGetPrimaryServiceResolveMap.set(resolveId, resolve);
        });
        console.always("get_primary_service result", result);
        return result;
      },
    });

    // /bluetooth_remote_gatt_service/get_characteristic
    this.registerCommonIpcOnMessageHandler({
      pathname: "/bluetooth_remote_gatt_service/get_characteristic",
      matchMode: "full",
      input: { uuid: "string" },
      output: "object",
      handler: async (args, ipc, request) => {
        if (this._browserWindow === undefined) {
          return { success: false, error: "没有开启蓝牙设备" };
        }
        if (this._apis === undefined) {
          return { success: false, error: "this._apis === undefined" };
        }
        const uuid = args.uuid;
        const resolveId = this._allocId++;
        this._maximize();
        this._apis.getCharacteristic(uuid, resolveId);
        const result = await new Promise((resolve) =>
          this._deviceGetCharacteristicResolveMap.set(resolveId, resolve)
        );
        console.always("getCharacteristic result", result);
        return result;
      },
    });
    this.registerCommonIpcOnMessageHandler({
      pathname: `/bluetooth_remote_gatt_characteristic/read_value`,
      matchMode: "full",
      input: {},
      output: "object",
      handler: async () => {
        console.always("bluetooth_remote_gatt_characteristic/read_value");
        if (this._browserWindow === undefined) {
          return { success: false, error: "没有开启蓝牙设备" };
        }
        if (this._apis === undefined) {
          return { success: false, error: "this._apis === undefined" };
        }
        const resolveId = this._allocId++;
        this._apis.characteristicRaadValue(resolveId);
        const result = await new Promise((resolve) =>
          this._deviceCharacteristicReadValueResolveMap.set(resolveId, resolve)
        );
        return result;
      },
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/bluetooth_remote_gatt_characteristic/get_descriptor",
      matchMode: "full",
      input: { uuid: "string" },
      output: "object",
      handler: async (args) => {
        if (this._browserWindow === undefined) {
          return { success: false, error: "没有开启蓝牙设备" };
        }
        if (this._apis === undefined) {
          return { success: false, error: "this._apis === undefined" };
        }
        const resolveId = this._allocId++;
        this._apis.characteristicGetDescriptor(args.uuid, resolveId);
        const result = await new Promise((resolve) => {
          this._characteristicGetDescriptorMap.set(resolveId, resolve);
        });
        return result;
      },
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/bluetooth_remote_gatt_descriptor/reaed_value",
      matchMode: "full",
      input: {},
      output: "object",
      handler: async () => {
        if (this._browserWindow === undefined) {
          return { success: false, error: "没有开启蓝牙设备" };
        }
        if (this._apis === undefined) {
          return { success: false, error: "this._apis === undefined" };
        }
        const resolveId = this._allocId++;
        this._apis.descriptorReadValue(resolveId);
        const result = await new Promise((resolve) => {
          this._descriptorReadValueMap.set(resolveId, resolve);
        });
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
          operationCallback: async (arg: unknown, resolveId: number) => {
            const resolve = this._operationResolveMap.get(resolveId);
            if (resolve === undefined) {
              throw new Error(`this._operationResolveMap.get(${resolveId})`);
            }
            resolve(arg);
            this._operationResolveMap.delete(resolveId);
          },
          deviceSelected: async (device: $Device) => {
            console.always("接受到了选择 device", device);
            if (this._bluetoothrequestdevicewatchSelectCallback === undefined) {
              this._apis?.deviceSelectedFailCallback();
              return;
            }
            this._bluetoothrequestdevicewatchSelectCallback(device.deviceId);
            this._bluetoothrequestdevicewatchSelectCallback = undefined;
          },
          // 设备连接成功
          deviceConnectedSuccess: async (server: BluetoothRemoteGATTServer) => {
            this._deviceConnectedResolve(server);
          },
          requestDeviceFail: async () => {
            this._requestDevice();
          },
          // 设备断开连接操作的回调
          deviceDisconnectCallback: async (arg: unknown, resolveId: number) => {
            const resolve = this._deviceDisconnectedResolveMap.get(resolveId);
            if (resolve === undefined) {
              throw new Error(
                `this.this._deviceDisconnectedResolveMap.get(${resolveId}) === undefined`
              );
            }
            resolve(arg);
            this._deviceDisconnectedResolveMap.delete(resolveId);
          },

          bluetoothRemoteGATTServerConnectCallback: async (
            arg: unknown,
            resolveId: number
          ) => {
            const resolve =
              this._bluetoothRemoteGATTServerConnectResolveMap.get(resolveId);
            if (resolve === undefined) {
              throw new Error(
                `this._bluetoothRemoteGATTServerConnectResolveMap.get(${resolveId}) === undefined`
              );
            }
            resolve(arg);
            this._bluetoothRemoteGATTServerConnectResolveMap.delete(resolveId);
          },

          deviceGetPrimaryServiceCallback: async (
            arg: unknown,
            resolveId: number
          ) => {
            const resolve =
              this._deviceGetPrimaryServiceResolveMap.get(resolveId);
            if (resolve === undefined) {
              throw new Error(
                `this._deviceGetPrimaryServiceResolveMap.get(resolveId) === undefined; resolveId === ${resolveId}`
              );
            }
            resolve(arg);
            this._deviceGetPrimaryServiceResolveMap.delete(resolveId);
          },
          deviceGetCharacteristicCallback: async (
            arg: unknown,
            resolveId: number
          ) => {
            const resolve =
              this._deviceGetCharacteristicResolveMap.get(resolveId);
            if (resolve === undefined) {
              throw new Error(
                `this._deviceGetCharacteristicResolveMap.get(resolveId) === undefiend; resolveId === ${resolveId}`
              );
            }
            resolve(arg);
            this._deviceGetCharacteristicResolveMap.delete(resolveId);
          },
          characteristicReadValueCallback: async (
            arg: unknown,
            resolveId: number
          ) => {
            const resolve =
              this._deviceCharacteristicReadValueResolveMap.get(resolveId);
            if (resolve === undefined) {
              throw new Error(
                `this._deviceCharacteristicReadValueResolveMap.get(resolveId) === undefiend; resolveId === ${resolveId}`
              );
            }
            resolve(arg);
            this._deviceCharacteristicReadValueResolveMap.delete(resolveId);
          },
          characteristicGetDescriptorCallback: async (
            arg: unknown,
            resolveId: number
          ) => {
            const resolve = this._characteristicGetDescriptorMap.get(resolveId);
            if (resolve === undefined) {
              throw new Error(
                `this._characteristicGetDescriptorMap.get(resolveId) === undefined; resolveId === ${resolveId}`
              );
            }
            resolve(arg);
            this._characteristicGetDescriptorMap.delete(resolveId);
          },
          descriptorReadValueCallback: async (
            arg: unknown,
            resolveId: number
          ) => {
            const resolve = this._descriptorReadValueMap.get(resolveId);
            if (resolve === undefined) {
              throw new Error(
                `this._descriptorReadValueMap.get(resolveId) === undefined; resolveId === ${resolveId}`
              );
            }
            resolve(arg);
            this._descriptorReadValueMap.delete(resolveId);
          },
        };
      }
    );
    bw.on("blur", () => {
      // this._minimize();
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
    this._apis = undefined;
  };

  // 最小化 UI
  // 一但最小化就会失去 连接
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
    // (await this._browserWindow).maximize();
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

/**
 * bluetooth.std.dweb UI 的状态
 * 分为
 * - closed 关闭关闭关闭状态
 *
 * - visible 可见状态
 * - hide 影藏状态
 */
