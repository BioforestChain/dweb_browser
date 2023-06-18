import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { _mmid_wapis_map } from "../../browser/multi-webview/mutil-webview.mobile.wapi.ts"
import { BrowserView } from "electron";
import { createHttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import { Ipc, IpcEvent, IpcRequest, IpcResponse } from "../../core/ipc/index.ts";
import { PromiseOut } from "../../helper/PromiseOut.ts";
import { Remote, expose, proxy, wrap } from "comlink";
 
type $APIS = typeof import("./assets/exportApis.ts")["APIS"];

export class BluetoothNMM extends NativeMicroModule {
  mmid = "bluetooth.std.dweb" as const;
  _bv: BrowserView | undefined; 
  _apis: unknown;
  _selectBluetoothCallback: {(id: string): void}  | undefined;
  // _observeBluetoothDevice 
  _bootstrap = async () => {
    console.always(`[${this.mmid} _bootstrap]`);

   
    const httpDwebServer = await createHttpDwebServer(this, {});
    (await httpDwebServer.listen()).onRequest(async (request, ipc) => {
      const url = "file:///sys/bluetooth" + request.parsed_url.pathname;
      console.always('url: ', url)
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
    


    /**
     * 查询全部的蓝牙设备 返回设备信息列表
     */
    this.registerCommonIpcOnMessageHandler({
      pathname: "/open",
      matchMode: "full",
      input: {},
      output: "object",
      handler: async (arg, ipc, request) => {
        // 打开一个browserView 
        await this._createBrowwerView(ipc, rootUrl)
        const apis = this._bv.getApis();
        this._bv.webContents.on('select-bluetooth-device', (event: Event, deviceList: $Device[], callback: {(id: string): void}) => {
          console.always("select-bluetooth-device; ")
          event.preventDefault()
          this._selectBluetoothCallback = callback
          console.log('deviceList: ', deviceList)
          apis.devicesUpdate(deviceList);
        })

        // apis.requestDevice();
        // console.always('open', rootUrl)
        // console.always('await', await apis.getName())
        return [];
      },
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/selected",
      matchMode: "full",
      input: { deviceId: "string"},
      output: "object",
      handler: (arg, ipc, request) => {
        console.always('close')
        return []
      },
    });

    /**
     * 关闭
     */
    this.registerCommonIpcOnMessageHandler({
      pathname: "/close",
      matchMode: "full",
      input: { deviceId: "string"},
      output: "object",
      handler: (arg, ipc, request) => {
        return []
      }, 
    });

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

  _createBrowwerView = async (ipc: Ipc, url: string) =>{
    const wapi = _mmid_wapis_map.get(ipc.remote.mmid);
    if(wapi === undefined) throw new Error(`nww === undefined`)
    const { MainPortToRenderPort } = await import("../../helper/electronPortMessage.ts");
    this._bv = new BrowserView({
      webPreferences: {
        sandbox: false,
        devTools: true,
        webSecurity: false,
        nodeIntegration: true,
        contextIsolation: false,
      }
    })

    const show_po = new PromiseOut<void>();
    const ports_po = new PromiseOut<{
      import_port: MessagePort;
      export_port: MessagePort;
    }>();
    this._bv.webContents.on('dom-ready', () => {
      show_po.resolve();
    })
    this._bv.webContents.ipc.once("renderPort", (event: MessageEvent) => {
      const [import_port, export_port] = event.ports;
      ports_po.resolve({
        import_port: MainPortToRenderPort(import_port),
        export_port: MainPortToRenderPort(export_port),
      });
    });


    wapi.nww.addBrowserView(this._bv);
    const bounds = wapi.nww.getBounds()
    const contentBounds = wapi.nww.getContentBounds();
    console.always("bounds.height - contentBounds.height: ", bounds ,contentBounds)
    await this._bv.webContents.loadURL(url)
    this._bv.setBounds({
      x: 0,
      y: bounds.height - contentBounds.height,
      width: contentBounds.width,
      height: contentBounds.height
    })

    this._bv.setTop
    await show_po.promise;
    this._bv.webContents.openDevTools({mode: "detach"})
    const { import_port, export_port } = await ports_po.promise;
    expose({
      deviceSelected: (device: $Device) => {
        this._selectBluetoothCallback? this._selectBluetoothCallback(device.deviceId) : "'";
      }
    }, export_port);

    return  Object.assign(this._bv, {
      getApis<T>() {
        return wrap<T>(import_port);
      },
    });
  }

  // _onConnect(_ipc: Ipc): void {
  //   _ipc.onEvent((message, ipc) => {
  //     // observe:bluetoothDevice
  //     console.always('bluttooth.main.ts 接受的奥了数据', message, _ipc.remote.mmid)
  //     if(message.name === "observe:bluetoothDevice"){
  //       // this.

  //     }
  //   })
  // }

  _shutdown = () => {
    throw new Error(`_shutdown 还没有处理`);
  };
}


export interface $Device{
  deviceId: string;
  deviceName: string;
}
