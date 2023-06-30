import type { Remote } from "comlink";
import type { $OnFetch, FetchEvent } from "../../core/helper/ipcFetchServer.ts";
import type { ReadableStreamIpc } from "../../core/ipc-web/index.ts";
import { Ipc, IpcHeaders, IpcResponse } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import type { $DWEB_DEEPLINK } from "../../core/types.ts";
import { createComlinkNativeWindow } from "../../helper/openNativeWindow.ts";
import {
  createHttpDwebServer,
  type HttpDwebServer,
} from "../../std/http/helper/$createHttpDwebServer.ts";
import { Router } from "./bluetooth.router.ts";
import { STATE } from "./const.ts";
import type { $Device, $ResponseJsonable } from "./types.ts";

type $APIS = typeof import("./assets/exportApis.ts")["APIS"];

export class BluetoothNMM extends NativeMicroModule {
  mmid = "bluetooth.std.dweb" as const;
  dweb_deeplinks = ["dweb:bluetooth"] as $DWEB_DEEPLINK[];
  private _router = new Router();
  private _responseHeader = new IpcHeaders().init(
    "Content-Type",
    "application/json"
  );
  private _STATE: STATE = STATE.CLOSED;
  private _apis: Remote<$APIS> | undefined;
  private _httpDwebServer: HttpDwebServer | undefined;
  private _wwwReadableStreamIpc: ReadableStreamIpc | undefined;
  private _browserWindow?: ReturnType<BluetoothNMM["_createBrowserWindow"]>;
  private _rootUrl = "";
  private _requestDeviceOptions: RequestDeviceOptions | undefined;
  private _allocId = 0;
  private _operationResolveMap: Map<number, { (arg: unknown): void }> =
    new Map();
  // 为 多次点击 设备列表做准备
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

  /**
   * this.onFetch 事件处理器
   * @param event
   * @returns
   */
  // private _onFetch: $OnFetch = async (event: FetchEvent) => {
  //   switch (event.method) {
  //     case IPC_METHOD.POST:
  //       return this._onFetchPOST(event);
  //     case IPC_METHOD.GET:
  //       return this._onfetchGET(event);
  //     default:
  //       return this._onfetchERR(event, 400, `没有匹配请求的路由`);
  //   }
  // };

  // /**
  //  * 全部 POST 请求的处理器
  //  * @param event
  //  * @returns
  //  */
  // private _onFetchPOST: $OnFetch = async (event: FetchEvent) => {
  //   switch (event.url.pathname) {
  //     case "/request_device":
  //       return this._requestDeviceHandler(event);
  //     default:
  //       return this._onfetchERR(event, 400, `没有匹配请求的路由`);
  //   }
  // };

  // /**
  //  * 全部 GET 请求的处理器
  //  * @param event
  //  * @returns
  //  */
  // private _onfetchGET: $OnFetch = async (event: FetchEvent) => {
  //   console.always("event.url", event.url);
  //   console.always("event.url.pathname", event.url.pathname);
  //   switch (event.url.pathname) {
  //     case "/open":
  //       return this._openHandler(event);
  //     case "bluetooth":
  //       return this.dwebBluetoothHandler(event);
  //     default:
  //       return this._onfetchERR(event, 400, `没有匹配请求的路由`);
  //   }
  // };

  private dwebBluetoothHandler: $OnFetch = async (event: FetchEvent) => {
    // 通过 deep_link 打开
    // 设备是 EDIFIER TWS NB2
    // 测试指令: deno task dnt --start bluetooth --acceptAllDevices true --optionalServices 00003802-0000-1000-8000-00805f9b34fb --optionalServices 00003802-0000-1000-8000-00805f9b34fb
    switch (event.url.protocol) {
      case "dweb:":
        return this._onFetchGET_bluetooth_withDwebProtocol(event);
    }
  };

  private _openHandler: $OnFetch = async (event: FetchEvent) => {
    let statusCode = 200;
    let jsonable: $ResponseJsonable = {
      success: true,
      error: undefined,
      data: undefined,
    };

    if (this._STATE === STATE.CLOSED) {
      const { bw, apis } = await this._initUI();
      await this._bluetoothrequestdevicewatch(bw, apis);
      this._STATE = STATE.HIDE;
    } else {
      statusCode = 429;
      jsonable = {
        success: false,
        error: `opened`,
        data: undefined,
      };
    }

    return IpcResponse.fromJson(
      event.ipcRequest.req_id,
      statusCode,
      this._responseHeader,
      jsonable,
      event.ipc
    );
  };

  private _requestDeviceHandler: $OnFetch = async (event: FetchEvent) => {
    this._requestDeviceOptions = await event.json();

    if (this._requestDeviceOptions === undefined) {
      return IpcResponse.fromJson(
        event.ipcRequest.req_id,
        422,
        this._responseHeader,
        {
          success: false,
          error: `illegal body`,
          data: undefined,
        },
        event.ipc
      );
    }

    if (this._STATE === STATE.CLOSED) {
      return IpcResponse.fromJson(
        event.ipcRequest.req_id,
        503,
        this._responseHeader,
        {
          success: false,
          error: `cannot requestDevice before opened`,
          data: undefined,
        },
        event.ipc
      );
    }

    if (this._browserWindow === undefined) {
      throw new Error("this._browserWindow === undefined");
    }

    // 展示 UI
    if (this._STATE === STATE.HIDE) {
      (await this._browserWindow)?.show();
      this._STATE = STATE.VISIBLE;
    }

    this._requestDevice();
    // 应该直接返回一个 reaableStream
    // 每次点击就可以不断地返回数据
    const res = await new Promise(
      (resolve) => (this._deviceConnectedResolve = resolve)
    );
    console.error(
      "error",
      "这里还需要调整???",
      "可以只有一次查询却能够多次点击 ？？？"
    );
    return IpcResponse.fromJson(
      event.ipcRequest.req_id,
      200,
      this._responseHeader,
      res,
      event.ipc
    );
  };

  /**
   * 通过 deep_link 打开 现阶段是测试使用的
   * @param event
   * @returns
   */
  private _onFetchGET_bluetooth_withDwebProtocol: $OnFetch = async (
    event: FetchEvent
  ) => {
    await await this._initUI();
    this._STATE = STATE.OPENED;
    const resolveId = this._allocId++;
    const acceptAllDevices = event.searchParams.get("acceptAllDevices");
    const options = {};
    if (acceptAllDevices !== null && acceptAllDevices === "true") {
      Reflect.set(options, "acceptAllDevices", true);
    }
    const optionalServices = event.searchParams.getAll("optionalServices");
    if (optionalServices !== null) {
      Reflect.set(options, "optionalServices", optionalServices);
    }
    this._requestDeviceOptions = options as RequestDeviceOptions;
    // 无法通过cli 直接 查询设备
    // this._requestDevice(resolveId);
    const result = await new Promise((resolve) =>
      this._operationResolveMap.set(resolveId, resolve)
    );
    return IpcResponse.fromJson(
      event.ipcRequest.req_id,
      200,
      this._responseHeader,
      JSON.stringify(result),
      event.ipc
    );
  };

  // private _initProcess = async (resolveId: number) => {
  //   await this._initUI();
  //   if (this._browserWindow === undefined)
  //     throw new Error(`this._browserWindow === undefined`);
  //   if (this._apis === undefined) throw new Error(`this._apis === undefined`);
  //   this._bluetoothrequestdevicewatch(await this._browserWindow, this._apis);
  //   this._requestDevice(resolveId);
  // };

  _bootstrap = async () => {
    console.always(`${this.mmid} _bootstrap`);
    // 创建服务
    this._rootUrl = await this._createHttpDwebServer();
    this._router.get("/open", this._openHandler);
    this._router.get("bluetooth", this.dwebBluetoothHandler);
    this._router.post("/request_device", this._requestDeviceHandler);
    this.onFetch(this._router.listen);
    // console.always("-------------");
    // this.registerCommonIpcOnMessageHandler({
    //   method: "POST",
    //   pathname: "/open",
    //   matchMode: "full",
    //   input: {},
    //   output: "object",
    //   handler: async (_, ipc, request) => {
    //     this._requestDeviceOptions = JSON.parse(await request.body.text());
    //     if (this._requestDeviceOptions === undefined) {
    //       return {
    //         success: false,
    //         error: `this._requestDeviceOptions === undefined`,
    //         data: undefined,
    //       };
    //     }

    //     if (this._browserWindow === undefined) {
    //       this._openUI();
    //     }
    //     // else {
    //     //   this._maximize();
    //     // }

    //     const res = await new Promise(
    //       (resolve) => (this._deviceConnectedResolve = resolve)
    //     );
    //     return res;
    //   },
    // });

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
        // this._openUI();
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
        show: false,
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
            // this._requestDevice();
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
  private _initUI = async () => {
    this._browserWindow = this._getBrowserWindow(this._rootUrl);
    this._apis = (await this._browserWindow).getApis<$APIS>();
    return {
      bw: await this._browserWindow,
      apis: this._apis,
    };
  };

  private _getBrowserWindow = (url: string, ipc?: Ipc) => {
    return (this._browserWindow ??= this._createBrowserWindow(url, ipc));
  };

  /**
   * 查询 设备
   * @param resolveId
   */
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
 * - hide 隐藏状态 当时
 */

// open 只是打开 模块
// close 关闭模块 关闭 UI
// requestDevice 显示 UI 查询列表

// 测试指令
// 设备是 EDIFIER TWS NB2
// deno task dnt --start bluetooth --acceptAllDevices true --optionalServices 00003802-0000-1000-8000-00805f9b34fb --optionalServices 00003802-0000-1000-8000-00805f9b34fb

// acceptAllDevices: true,
// optionalServices: ["00003802-0000-1000-8000-00805f9b34fb"],
