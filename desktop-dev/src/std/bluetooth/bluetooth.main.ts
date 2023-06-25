import { BrowserView } from "electron";
import { ALL_MMID_MWEBVIEW_WINDOW_MAP } from "../../browser/multi-webview/multi-webview.mobile.wapi.ts";
import { Ipc, IpcResponse } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { createComlinkNativeWindow } from "../../helper/openNativeWindow.ts";
import { createHttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import type { $Device } from "./types.ts";
import type { Remote } from "comlink";

type $APIS = typeof import("./assets/exportApis.ts")["APIS"];

export class BluetoothNMM extends NativeMicroModule {
  mmid = "bluetooth.std.dweb" as const;
  _bv: BrowserView | undefined;
  _apis: Remote<$APIS> | undefined;
  _selectBluetoothCallback: { (id: string): void } | undefined;
  // _observeBluetoothDevice
  _bootstrap = async () => {
    console.always(`[${this.mmid} _bootstrap]`);

    // 创建服务
    const httpDwebServer = await createHttpDwebServer(this, {});
    (await httpDwebServer.listen()).onRequest(async (request, ipc) => {
      const url = "file:///sys/bluetooth" + request.parsed_url.pathname;
      console.always("url: ", url);
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
    
    const rootUrl = httpDwebServer.startResult.urlInfo.buildInternalUrl(
      (url) => {
        url.pathname = "/index.html";
      }
    ).href;

    await this._getBrowwerView(rootUrl);
    this._apis = (await this._browserWindow)?.getApis();
    // const apis = this._bv.getApis();
    // this._bv.webContents.on(
    //   "select-bluetooth-device",
    //   (
    //     event: Event,
    //     deviceList: $Device[],
    //     callback: { (id: string): void }
    //   ) => {
    //     console.always("select-bluetooth-device; ");
    //     event.preventDefault();
    //     this._selectBluetoothCallback = callback;
    //     console.log("deviceList: ", deviceList);
    //     apis.devicesUpdate(deviceList);
    //   }
    // );

    // 先注册处理器
    this.registerCommonIpcOnMessageHandler({
      method: "POST", 
      pathname: "/device_list_update",
      matchMode: "full",
      input: {},
      output: "object",
      handler: async (arg, ipc, request) => {
        const deviceList: any = JSON.parse(await request.body.text());
        // console.always("bluetooth.std.dweb device_list_update: ", Date.now());
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

    // /**
    //  * 查询全部的蓝牙设备 返回设备信息列表
    //  */
    // this.registerCommonIpcOnMessageHandler({
    //   pathname: "/open",
    //   matchMode: "full",
    //   input: {},
    //   output: "object",
    //   handler: async (arg, ipc, request) => {
    //     // 打开一个browserView
    //     await this._getBrowwerView(rootUrl, ipc);
    //     const apis = this._bv.getApis();
    //     this._bv.webContents.on(
    //       "select-bluetooth-device",
    //       (
    //         event: Event,
    //         deviceList: $Device[],
    //         callback: { (id: string): void }
    //       ) => {
    //         console.always("select-bluetooth-device; ");
    //         event.preventDefault();
    //         this._selectBluetoothCallback = callback;
    //         console.log("deviceList: ", deviceList);
    //         apis.devicesUpdate(deviceList);
    //       }
    //     );

    //     apis.requestDevice();

    //     // apis.requestDevice();
    //     // console.always('open', rootUrl)
    //     // console.always('await', await apis.getName())
    //     return [];
    //   },
    // });

    // this.registerCommonIpcOnMessageHandler({
    //   pathname: "/selected",
    //   matchMode: "full",
    //   input: { deviceId: "string" },
    //   output: "object",
    //   handler: (arg, ipc, request) => {
    //     console.always("close");
    //     return [];
    //   },
    // });


    

    /**
     * 获取蓝牙设备相关的信息
     */
    // this.registerCommonIpcOnMessageHandler({
    //   pathname: "/get_characteristic_value",
    //   matchMode: "full",
    //   input: { service: "string" | number, characteristic: "string" | number},
    //   output: "object",
    //   handler: (arg, ipc, request) => {

    //   },
    // });
  };
  /**
   * 创建一个新的隐藏窗口装载webview，使用它的里头 web-bluetooth-api 来实现我们的需求
   * @param url
   * @param ipc
   * @returns
   */
  private _createBrowserView = async (url: string, ipc?: Ipc) => createComlinkNativeWindow(
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
          // 关闭自己
          this._closeUI();
        },
      };
    }
  );

  // 关闭 UI 
  private _closeUI = async () => {
    if(this._browserWindow === undefined) throw new Error("this._browserWindow === undefined");
    (await this._browserWindow).destroy();
  }

  private _browserWindow?: ReturnType<BluetoothNMM["_createBrowserView"]>;
  private _getBrowwerView = (url: string, ipc?: Ipc) => {
    return (this._browserWindow ??= this._createBrowserView(url, ipc));
  };

  async _openNativeWindow() {
    await this.nativeFetch(`file://mwebview.browser.dweb/open?url=about:blank`);
    return ALL_MMID_MWEBVIEW_WINDOW_MAP.get(this.mmid)!;
  }

  protected override async _shutdown() {
    const window = await this._browserWindow;
    window?.close();
  }
}
