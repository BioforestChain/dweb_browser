import type { Remote } from "comlink";
import { TextEncoder } from "https://deno.land/std@0.177.0/node/util.ts";
import type { $OnFetch, FetchEvent } from "../../core/helper/ipcFetchHelper.ts";
import { Ipc, IpcHeaders, IpcResponse } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import type { $DWEB_DEEPLINK, $MMID } from "../../core/types.ts";
import { createComlinkNativeWindow } from "../../helper/openNativeWindow.ts";
import { createHttpDwebServer, type HttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
// import { OnFetchAdapter } from "./bluetooth.onFetchAdapter.ts";
import { OnFetchAdapter } from "../../helper/onFetchAdapter.ts";
import { fetchMatch } from "../../helper/patternHelper.ts";
import { STATE } from "./const.ts";
import type { $AllWatchControllerItem, $Device, $ResponseJsonable, RequestDeviceOptions } from "./types.ts";

type $APIS = typeof import("./assets/exportApis.ts")["APIS"];

export class BluetoothNMM extends NativeMicroModule {
  mmid = "bluetooth.std.dweb" as const;
  dweb_deeplinks = ["dweb:bluetooth"] as $DWEB_DEEPLINK[];
  private _encode = new TextEncoder().encode;
  // private _router = new Router();
  private _onFetchAdapter = new OnFetchAdapter();
  private _responseHeader = new IpcHeaders().init("Content-Type", "application/json");
  private _STATE: STATE = STATE.CLOSED;
  private _apis: Remote<$APIS> | undefined;
  private _httpDwebServer: HttpDwebServer | undefined;
  private _browserWindow?: ReturnType<BluetoothNMM["_createBrowserWindow"]>;
  private _rootUrl = "";
  private _requestDeviceOptions: RequestDeviceOptions | undefined;
  private _allocId = 0;
  private _operationResolveMap: Map<number, { (arg: $ResponseJsonable<unknown>): void }> = new Map();
  // 为 多次点击 设备列表做准备
  private _deviceConnectedResolve(value: $ResponseJsonable<unknown>) {}
  // private _deviceDisconnectedResolve(value: unknown) {}
  private _deviceDisconnectedResolveMap: Map<number, { (arg: unknown): void }> = new Map();
  private _bluetoothRemoteGATTServerConnectResolveMap: Map<number, { (arg: unknown): void }> = new Map();
  private _deviceGetPrimaryServiceResolveMap: Map<number, { (arg: unknown): void }> = new Map();
  private _deviceGetCharacteristicResolveMap: Map<number, { (arg: unknown): void }> = new Map();
  private _deviceCharacteristicReadValueResolveMap: Map<number, { (arg: unknown): void }> = new Map();
  private _characteristicGetDescriptorMap: Map<number, { (arg: unknown): void }> = new Map();
  private _descriptorReadValueMap: Map<number, { (arg: unknown): void }> = new Map();
  private _bluetoothrequestdevicewatchSelectCallback: { (deviceId: string): void } | undefined;
  // 全部的 watchController 用来向 client 发送数据；
  private _allWatchController = new Map<$MMID, $AllWatchControllerItem>();

  _bootstrap = async () => {
    console.always(`${this.mmid} _bootstrap`);
    await this._createHttpDwebServer();
    const onFetch = fetchMatch()
      .get("/open", this._openHandler)
      .get("/close", this._closeHandler)
      .get("/watch", this._watchHandler)
      .get("bluetooth", this.dwebBluetoothHandler)
      .post("/request_connect_device", this._requestAndConnectDeviceHandler)
      .get("/bluetooth_device/forget", this._bluetoothDevice_forget)
      .get("/bluetooth_device/watch_advertisements", this._bluetoothDevice_watchAdvertisements)
      .get("/bluetooth_remote_gatt_server/connect", this._bluetoothRemoteGATTServer_connect)
      .get("/bluetooth_remote_gatt_server/disconnect", this._bluetoothRemoteGATTServer_disconnect)
      .get("/bluetooth_remote_gatt_server/get_primary_service", this._bluetoothRemoteGATTServer_getPrimaryService)
      .get("/bluetooth_remote_gatt_service/get_characteristic", this._bluetoothRemoteGATTService_getCharacteristic)
      .get("/bluetooth_remote_gatt_characteristic/read_value", this._bluetoothRemoteGATTCharacteristic_readValue)
      .post("/bluetooth_remote_gatt_characteristic/write_value", this._bluetoothRemoteGATTCharacteristic_writeValue)
      .get(
        "/bluetooth_remote_gatt_characteristic/get_descriptor",
        this._bluetoothRemoteGATTCharacteristic_getDescriptor
      )
      .post(`/bluetooth_remote_gatt_descriptor/reaed_value`, this._bluetoothRemoteGATTDescriptor_readValue)
      .post(`/bluetooth_remote_gatt_descriptor/write_value`, this._bluetoothRemoteGATTDescriptor_writeValue);
    this.onFetch(onFetch.run);
  };

  _exports = {
    operationCallback: async (arg: $ResponseJsonable<unknown>, resolveId: number) => {
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

    // 设备连接的回调函数
    deviceConnectedCallback: async (
      // server: BluetoothRemoteGATTServer
      res: $ResponseJsonable<unknown>
    ) => {
      this._deviceConnectedResolve(res);
    },

    // 设备断开连接操作的回调
    deviceDisconnectCallback: async (arg: unknown, resolveId: number) => {
      const resolve = this._deviceDisconnectedResolveMap.get(resolveId);
      if (resolve === undefined) {
        throw new Error(`this.this._deviceDisconnectedResolveMap.get(${resolveId}) === undefined`);
      }
      resolve(arg);
      this._deviceDisconnectedResolveMap.delete(resolveId);
    },

    // 监听 bluetoothRemoteGATTService 的状态变化
    bluetoothRemoteGATTServiceListenner: async (type: string, event: Event) => {
      console.log("", "bluetoothRemoteGATTServiceListenner 执行了", type, event);
    },

    // bluetooth 状态发生变化回调
    watchStateChange: async (
      type: $AllWatchControllerItem.$SendParam["type"],
      data: $AllWatchControllerItem.$SendParam["data"]
    ) => {
      this._allWatchController.forEach((item: $AllWatchControllerItem) => item.send({ type, data }));
    },
  };

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
    if (this._STATE === STATE.CLOSED) {
      const { bw, apis } = await this._initUI();
      await this._bluetoothrequestdevicewatch(bw, apis);
      this._STATE = STATE.HIDE;
    } else {
      return this._createResponseError(event, 429, `bluetooth.std.dweb is opened!`);
    }
    return this._createResponseSucess(event, {
      success: true,
      error: undefined,
      data: undefined,
    });
  };

  private _closeHandler: $OnFetch = async (event: FetchEvent) => {
    if (this._STATE === STATE.CLOSED) {
      return this._createResponseError(event, 200, `bluetooth.std.dweb not yet opened!`);
    }

    if (this._browserWindow === undefined) {
      return this._createResponseError(event, 200, `broserWindow === undefined!`);
    }

    this._closeUI(await this._browserWindow);
    return this._createResponseSucess(event, {
      success: true,
      error: undefined,
      data: "ok",
    });
  };

  private _watchHandler: $OnFetch = async (event: FetchEvent) => {
    const remoteMMid = event.ipcRequest.ipc.remote.mmid;
    //测试返回一个 stream
    const readableStream = new ReadableStream({
      start: (controller) => {
        this._allWatchController.set(remoteMMid, {
          controller,
          send: (jsonable: $AllWatchControllerItem.$SendParam) => {
            const data = new TextEncoder().encode(JSON.stringify(jsonable));
            controller.enqueue(data);
          },
        });
      },
      pull(_controller) {},
      cancel: (reson) => {
        console.log("", "cancel", reson);
        this._allWatchController.delete(remoteMMid);
      },
    });

    // 读取发送过来的数据的方法
    (async () => {
      const stream = event.ipcRequest.body.stream();
      const reader = (await stream).getReader();
      let loop = true;
      while (loop) {
        const { done } = await reader.read();
        loop = !done;
      }
      // client 关闭了 ws 会执行到这里
      this._allWatchController.delete(remoteMMid);
    })();

    return IpcResponse.fromStream(
      event.ipcRequest.req_id,
      200,
      new IpcHeaders().init("Content-Type", "application/octet-stream"),
      readableStream,
      event.ipc
    );
  };

  /**
   * 查询蓝牙设备
   * 业务逻辑
   * 显示 UI 列表 -> 点击UI列表中的项,发起连接 -> 连接成功 -> 返回一个 GATTServer 数据
   * -> 连接失败 -> 返回一个 失败的数据
   * 查询一次 只能够 进行一次选择
   *
   * @param event
   * @returns
   */
  private _requestAndConnectDeviceHandler: $OnFetch = async (event: FetchEvent) => {
    this._requestDeviceOptions = await event.json();
    if (this._requestDeviceOptions === undefined) {
      return this._createResponseError(event, 422, `illegal body`);
    }

    if (this._STATE === STATE.CLOSED) {
      return this._createResponseError(event, 503, `cannot requestDevice before opened`);
    }

    if (this._browserWindow === undefined) {
      throw new Error("this._browserWindow === undefined");
    }

    // 展示 UI
    if (this._STATE === STATE.HIDE) {
      (await this._browserWindow)?.show();
      this._STATE = STATE.VISIBLE;
    }

    const resPromise = new Promise<$ResponseJsonable<unknown>>((resolve) => (this._deviceConnectedResolve = resolve));
    this._requestDevice();
    const res = await resPromise;
    (await this._browserWindow)?.hide();
    this._STATE = STATE.HIDE;
    return this._createResponseSucess(event, res);
  };

  private _bluetoothDevice_forget: $OnFetch = async (event: FetchEvent) => {
    if (this._STATE === STATE.CLOSED) {
      return this._createResponseError(event, 200, `bluetooth.std.dweb not yet opened!`);
    }
    const id = event.url.searchParams.get("id");
    if (id === null) {
      return this._createResponseError(event, 422, `missing id parameter!`);
    }
    const resolveId = this._allocId++;
    const resPromise = new Promise<$ResponseJsonable<unknown>>((resolve) =>
      this._operationResolveMap.set(resolveId, resolve)
    );
    this._apis?.bluetoothDeviceForget(id, resolveId);
    const res = await resPromise;
    return this._createResponseSucess(event, res);
  };

  private _bluetoothDevice_watchAdvertisements: $OnFetch = async (event: FetchEvent) => {
    if (this._STATE === STATE.CLOSED) {
      return this._createResponseError(event, 200, `bluetooth.std.dweb not yet opened!`);
    }
    const id = event.url.searchParams.get("id");
    if (id === null) {
      return this._createResponseError(event, 422, `missing id parameter!`);
    }
    const resolveId = this._allocId++;
    const resPromise = new Promise<$ResponseJsonable<unknown>>((resolve) =>
      this._operationResolveMap.set(resolveId, resolve)
    );
    this._apis?.bluetoothDeviceWatchAdvertisements(id, resolveId);
    const res = await resPromise;
    return this._createResponseSucess(event, res);
  };

  private _bluetoothRemoteGATTServer_connect: $OnFetch = async (event: FetchEvent) => {
    if (this._STATE === STATE.CLOSED) {
      return this._createResponseError(event, 200, `bluetooth.std.dweb not yet opened!`);
    }
    const id = event.url.searchParams.get("id");
    if (id === null) {
      return this._createResponseError(event, 422, `missing id parameter!`);
    }
    const resolveId = this._allocId++;
    const resPromise = new Promise<$ResponseJsonable<unknown>>((resolve) =>
      this._operationResolveMap.set(resolveId, resolve)
    );
    this._apis?.bluetoothRemoteGATTServerConnect(id, resolveId);
    const res = await resPromise;
    return this._createResponseSucess(event, res);
  };

  private _bluetoothRemoteGATTServer_disconnect: $OnFetch = async (event: FetchEvent) => {
    if (this._STATE === STATE.CLOSED) {
      return this._createResponseError(event, 200, `bluetooth.std.dweb not yet opened!`);
    }
    const id = event.url.searchParams.get("id");
    if (id === null) {
      return this._createResponseError(event, 422, `missing id parameter!`);
    }
    const resolveId = this._allocId++;
    const resPromise = new Promise<$ResponseJsonable<unknown>>((resolve) =>
      this._operationResolveMap.set(resolveId, resolve)
    );
    this._apis?.bluetoothRemoteGATTServerDisconnect(id, resolveId);
    const res = await resPromise;
    return this._createResponseSucess(event, res);
  };

  private _bluetoothRemoteGATTServer_getPrimaryService: $OnFetch = async (event: FetchEvent) => {
    if (this._STATE === STATE.CLOSED) {
      return this._createResponseError(event, 200, `bluetooth.std.dweb not yet opened!`);
    }
    const uuid = event.url.searchParams.get("uuid");
    if (uuid === null) {
      return this._createResponseError(event, 422, `missing uuid parameter!`);
    }
    const resolveId = this._allocId++;
    const resPromise = new Promise<$ResponseJsonable<unknown>>((resolve) =>
      this._operationResolveMap.set(resolveId, resolve)
    );
    this._apis?.bluetoothRemoteGATTServerGetPrimarySevice(uuid, resolveId);
    const res = await resPromise;
    console.log("", "getPrimaryService end");
    return this._createResponseSucess(event, res);
  };

  private _bluetoothRemoteGATTService_getCharacteristic: $OnFetch = async (event: FetchEvent) => {
    if (this._STATE === STATE.CLOSED) {
      return this._createResponseError(event, 200, `bluetooth.std.dweb not yet opened!`);
    }
    const uuid = event.url.searchParams.get("uuid");
    if (uuid === null) {
      return this._createResponseError(event, 422, `missing uuid parameter!`);
    }
    const resolveId = this._allocId++;
    const resPromise = new Promise<$ResponseJsonable<unknown>>((resolve) =>
      this._operationResolveMap.set(resolveId, resolve)
    );
    this._apis?.bluetoothRemoteGATTService_getCharacteristic(uuid, resolveId);
    const res = await resPromise;
    return this._createResponseSucess(event, res);
  };

  private _bluetoothRemoteGATTCharacteristic_getDescriptor: $OnFetch = async (event: FetchEvent) => {
    if (this._STATE === STATE.CLOSED) {
      return this._createResponseError(event, 200, `bluetooth.std.dweb not yet opened!`);
    }
    const uuid = event.url.searchParams.get("uuid");
    if (uuid === null) {
      return this._createResponseError(event, 422, `missing uuid parameter!`);
    }
    const resolveId = this._allocId++;
    const resPromise = new Promise<$ResponseJsonable<unknown>>((resolve) =>
      this._operationResolveMap.set(resolveId, resolve)
    );
    this._apis?.BluetoothRemoteGATTCharacteristic_getDescriptor(uuid, resolveId);
    const res = await resPromise;
    return this._createResponseSucess(event, res);
  };

  private _bluetoothRemoteGATTDescriptor_readValue: $OnFetch = async (event: FetchEvent) => {
    const resolveId = this._allocId++;
    const resPromise = new Promise<$ResponseJsonable<unknown>>((resolve) =>
      this._operationResolveMap.set(resolveId, resolve)
    );
    this._apis?.bluetoothRemoteGATTDescriptor_readValue(resolveId);
    const res = await resPromise;
    // res.data === daaValue
    return this._createResponseSucess(event, res);
  };

  private _bluetoothRemoteGATTDescriptor_writeValue: $OnFetch = async (event: FetchEvent) => {
    const arrayBuffer = await event.request.arrayBuffer();
    const resolveId = this._allocId++;
    const resPromise = new Promise<$ResponseJsonable<unknown>>((resolve) =>
      this._operationResolveMap.set(resolveId, resolve)
    );
    this._apis?.bluetoothRemoteGATTDescriptor_writeValue(arrayBuffer, resolveId);
    const res = await resPromise;
    return this._createResponseSucess(event, res);
  };

  private _bluetoothRemoteGATTCharacteristic_readValue: $OnFetch = async (event: FetchEvent) => {
    if (this._STATE === STATE.CLOSED) {
      return this._createResponseError(event, 200, `bluetooth.std.dweb not yet opened!`);
    }
    const resolveId = this._allocId++;
    const resPromise = new Promise<$ResponseJsonable<unknown>>((resolve) =>
      this._operationResolveMap.set(resolveId, resolve)
    );
    this._apis?.bluetoothRemoteGATTCharacteristic_readValue(resolveId);
    const res = await resPromise;
    return this._createResponseSucess(event, res);
  };

  private _bluetoothRemoteGATTCharacteristic_writeValue: $OnFetch = async (event: FetchEvent) => {
    if (this._STATE === STATE.CLOSED) {
      return this._createResponseError(event, 200, `bluetooth.std.dweb not yet opened!`);
    }
    const arrayBuffer = await event.request.arrayBuffer();
    const resolveId = this._allocId++;
    const resPromise = new Promise<$ResponseJsonable<unknown>>((resolve) =>
      this._operationResolveMap.set(resolveId, resolve)
    );
    this._apis?.bluetoothRemoteGATTCharacteristic_writeValue(arrayBuffer, resolveId);
    const res = await resPromise;
    return this._createResponseSucess(event, res);
  };

  /**
   * 通过 deep_link 打开 现阶段是测试使用的
   * @param event
   * @returns
   */
  private _onFetchGET_bluetooth_withDwebProtocol: $OnFetch = async (event: FetchEvent) => {
    await await this._initUI();
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
    const result = await new Promise((resolve) => this._operationResolveMap.set(resolveId, resolve));
    return IpcResponse.fromJson(event.ipcRequest.req_id, 200, this._responseHeader, JSON.stringify(result), event.ipc);
  };

  /**
   * 创建服务
   * @returns
   */
  private _createHttpDwebServer = async () => {
    this._httpDwebServer = await createHttpDwebServer(this, {});
    const serverIpc = await this._httpDwebServer.listen();
    serverIpc.onFetch(async (event) => {
      return await this.nativeFetch("file:///sys/bluetooth" + event.pathname);
    });
    this._rootUrl = this._httpDwebServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href;
    return this;
  };

  /**
   * 初始化适配器
   * @returns
   */
  // private _onFetchAdapterInit() {
  //   this._onFetchAdapter
  //     // .add("GET", "/open", this._openHandler)
  //     // .add("GET", "/close", this._closeHandler)
  //     // .add("GET", "/watch", this._watchHandler)
  //     // .add("GET", "bluetooth", this.dwebBluetoothHandler)
  //     // .add(
  //     //   "POST",
  //     //   "/request_connect_device",
  //     //   this._requestAndConnectDeviceHandler
  //     // )
  //     // .add("GET", "/bluetooth_device/forget", this._bluetoothDevice_forget)
  //     // .add(
  //     //   "GET",
  //     //   "/bluetooth_device/watch_advertisements",
  //     //   this._bluetoothDevice_watchAdvertisements
  //     // )
  //     // .add(
  //     //   "GET",
  //     //   "/bluetooth_remote_gatt_server/connect",
  //     //   this._bluetoothRemoteGATTServer_connect
  //     // )
  //     // .add(
  //     //   "GET",
  //     //   "/bluetooth_remote_gatt_server/disconnect",
  //     //   this._bluetoothRemoteGATTServer_disconnect
  //     // )

  //     // .add(
  //     //   "GET",
  //     //   "/bluetooth_remote_gatt_server/get_primary_service",
  //     //   this._bluetoothRemoteGATTServer_getPrimaryService
  //     // )
  //     // .add(
  //     //   "GET",
  //     //   "/bluetooth_remote_gatt_service/get_characteristic",
  //     //   this._bluetoothRemoteGATTService_getCharacteristic
  //     // )
  //     // .add(
  //     //   "GET",
  //     //   "/bluetooth_remote_gatt_characteristic/read_value",
  //     //   this._bluetoothRemoteGATTCharacteristic_readValue
  //     // )
  //     // .add(
  //     //   "POST",
  //     //   "/bluetooth_remote_gatt_characteristic/write_value",
  //     //   this._bluetoothRemoteGATTCharacteristic_writeValue
  //     // )
  //     // .add(
  //     //   "GET",
  //     //   "/bluetooth_remote_gatt_characteristic/get_descriptor",
  //     //   this._bluetoothRemoteGATTCharacteristic_getDescriptor
  //     // )
  //     // .add(
  //     //   "GET",
  //     //   `/bluetooth_remote_gatt_descriptor/reaed_value`,
  //     //   this._bluetoothRemoteGATTDescriptor_readValue
  //     // )
  //     // .add(
  //     //   "POST",
  //     //   `/bluetooth_remote_gatt_descriptor/write_value`,
  //     //   this._bluetoothRemoteGATTDescriptor_writeValue
  //     // );
  //   return this;
  // }

  // /**
  //  * 开始监听请求
  //  */
  // private listenRequest() {
  //   this.onFetch(this._onFetchAdapter.run);
  // }

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
      async (win) => this._exports
    );
    return bw;
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
    this._apis = (await this._browserWindow).getRenderApi<$APIS>();
    this._STATE = STATE.OPENED;
    (await this._browserWindow).on("blur", async () => {
      (await this._browserWindow)?.hide();
      this._STATE = STATE.HIDE;
    });
    return {
      bw: await this._browserWindow,
      apis: this._apis,
    };
  };

  // 关闭 UI
  private _closeUI = async (bw: Electron.BrowserWindow) => {
    bw.close();
    this._browserWindow = undefined;
    this._apis = undefined;
    this._STATE = STATE.CLOSED;
  };

  private _getBrowserWindow = (url: string, ipc?: Ipc) => {
    return (this._browserWindow ??= this._createBrowserWindow(url, ipc));
  };

  /**
   * 查询 设备
   * @param resolveId
   */
  private _requestDevice = async () => {
    if (this._browserWindow === undefined) throw new Error(`this._browserWindow === undefined`);
    (await this._browserWindow).webContents.executeJavaScript(
      `requestDevice(${JSON.stringify(this._requestDeviceOptions)})`,
      true
    );
  };

  private _bluetoothrequestdevicewatch = async (bw: Electron.BrowserWindow, apis: Remote<$APIS>) => {
    bw.webContents.on("select-bluetooth-device", async (event, deviceList, callback) => {
      console.always("select-bluetooth-device; ", Date.now());
      event.preventDefault();
      this._bluetoothrequestdevicewatchSelectCallback = callback;
      apis.devicesUpdate(deviceList);
    });
  };

  private _createResponseError = (event: FetchEvent, statusCode: number, errStr: string) => {
    return IpcResponse.fromJson(
      event.ipcRequest.req_id,
      statusCode,
      this._responseHeader,
      {
        success: false,
        error: errStr,
        data: undefined,
      },
      event.ipc
    );
  };

  private _createResponseSucess = (event: FetchEvent, res: unknown) => {
    return IpcResponse.fromJson(event.ipcRequest.req_id, 200, this._responseHeader, res, event.ipc);
  };

  protected override _shutdown = async () => {
    this._httpDwebServer?.close();
    if (this._browserWindow) {
      this._closeUI(await this._browserWindow);
    }
  };
}

class Execute {
  private _callbackList: $ExcuteCallback[] = [];
  private _preRes: any;
  addProcess(callback: $ExcuteCallback) {
    this._callbackList.push(callback);
  }

  async excute() {
    for (const index in this._callbackList) {
      const { err, res } = await this._callbackList[index](this._preRes);
      if (err) {
        return res;
      }
      this._preRes = res;
    }
    return this._preRes;
  }
}

export interface $ExcuteCallback {
  (...arg: any[]): any;
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
