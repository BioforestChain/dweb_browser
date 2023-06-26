import type { OutgoingMessage } from "node:http";
import type { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { Ipc, IpcEvent, IpcRequest } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { $Schema1ToType } from "../../helper/types.ts";
import {
  ALL_MMID_MWEBVIEW_WINDOW_MAP,
  getMWebViewWindow,
  //   deleteWapis,
  //   forceGetWapis,
  //   getAllWapis,
  getOrOpenMWebViewWindow,
} from "./multi-webview.mobile.wapi.ts";
import { $AllWebviewState } from "./types.ts";

/**
 * 构建一个视图树
 * 如果是桌面版，所以不用去管树的概念，直接生成生成就行了
 * 但这里是模拟手机版，所以还是构建一个层级视图
 */
export class MultiWebviewNMM extends NativeMicroModule {
  mmid = "mwebview.browser.dweb" as const;
  observeMap: $ObserveMapNww = new Map();
  encoder = new TextEncoder();
  async _bootstrap(_context: $BootstrapContext) {
    console.always(`${this.mmid} _bootstrap`);

    this.registerCommonIpcOnMessageHandler({
      pathname: "/open",
      matchMode: "full",
      input: { url: "string" },
      output: "number",
      handler: (...args) => this._open(...args),
    });

    // 激活窗口
    this.registerCommonIpcOnMessageHandler({
      pathname: "/activate",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: async (_, client_ipc) => {
        const wmm = await getMWebViewWindow(client_ipc);
        if (wmm === undefined) {
          return false;
        }
        wmm.win.focus();
        return true;
      },
    });

    /**
     * 关闭当前激活的window
     * ps: 在桌面端中，每个app的前端页面承载的对象是window,在android则是activity，
     * 每个app都有个单独的service,我们承载在worker里面，这里是只关闭app的前端页面，也即关闭window
     * 每个app只能关闭自己的window
     */
    this.registerCommonIpcOnMessageHandler({
      pathname: "/close/window",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: async (_, client_ipc) => {
        const wmm = await getMWebViewWindow(client_ipc);
        if (wmm === undefined) {
          return false;
        }
        wmm.win.close();
        return true;
      },
    });

    // 销毁指定的 webview
    this.registerCommonIpcOnMessageHandler({
      pathname: "/destroy_webview_by_host",
      matchMode: "full",
      input: { host: "string" },
      output: "boolean",
      handler: async (args, ipc) => {
        const mww = await getMWebViewWindow(ipc);
        let changed = false;
        if (mww) {
          for (const viewItem of mww.getAllBrowserView()) {
            const url = new URL(viewItem.view.webContents.getURL());
            if (url.host === args.host) {
              changed = true;
              mww.deleteBrowserView(viewItem.view);
            }
          }
        }
        return changed;
      },
    });

    // 选择指定的 蓝牙设备
    this.registerCommonIpcOnMessageHandler({
      pathname: "/bluetooth/device/selected",
      matchMode: "full",
      input: {id: "string"},
      output: "boolean",
      handler: async (args, ipc) => {
        console.error("error",'args.id', args.id)
        if(this._bluetoothrequestdevicewatchSelectCallback){
          this._bluetoothrequestdevicewatchSelectCallback(args.id)
          this._bluetoothrequestdevicewatchSelectCallback = undefined;
          return true
        }else{
          return false
        }
      }
    })

    // 同步是否可以不需要要了？？
    // 需要修改 通过 webview 需要 区分 ipc or window 来
    // 需要根据 ipc.uid 决定向那个 ipc 发送同步的数据
    // 需要需要包括 webview_id isActive statusbar 等状态
    //
    Electron.ipcMain.on(
      "sync:webview_state",
      (
        event: Electron.IpcMainEvent,
        uid: string,
        allWebviewState: $AllWebviewState
      ) => {
        const ipc = this._all_open_ipc.get(parseInt(uid));
        if (ipc === undefined) {
          // throw new Error(`sync:webview_state ipc === undefined`)
          // 暂时来说 只有通过 _open 打开的需要 才能监听
          return;
        } else {
          ipc.postMessage(
            IpcEvent.fromText("state", JSON.stringify(allWebviewState))
          );
        }
      }
    );
  }

  private _all_open_ipc = new Map<number, Ipc>();
  /**
   * 打开 应用
   * 如果 是由 jsProcess 调用 会在当前的 browserWindow 打开一个新的 webview
   * 如果 是由 NMM 调用的 会打开一个新的 borserWindow 同时打开一个新的 webview
   */
  private async _open(
    args: $Schema1ToType<{ url: "string" }>,
    clientIpc: Ipc,
    _request: IpcRequest
  ) {
    const mww = await getOrOpenMWebViewWindow(clientIpc);
    const view = mww.createBrowserView(args.url);
    // 测试代码部分
    this._bluetoothrequestdevicewatch(view)
    // 测试代码部分
    return view.webContents.id;
  }

  protected override async _shutdown() {
    for (const [mmid, mwwP] of ALL_MMID_MWEBVIEW_WINDOW_MAP) {
      const mww = await mwwP;
      mww.win.close();
    }
  }

  private _bluetoothrequestdevicewatchSelectCallback: {(deviceId: string): void} | undefined;
  private _bluetoothrequestdevicewatch(veiw: Electron.BrowserView){
    veiw.webContents.on(
      "select-bluetooth-device",
      async (
        event: Event,
        deviceList: any[],
        callback: { (id: string): void }
      ) => {
        console.always("select-bluetooth-device; ", Date.now());
        event.preventDefault();
        this._bluetoothrequestdevicewatchSelectCallback = callback;
        this.nativeFetch(
          "file://bluetooth.std.dweb/device_list_update",
          {
            method: "POST",
            body: JSON.stringify(deviceList)
          }
        )
      }
    );
  }
}

export interface $ObserveItem {
  res: OutgoingMessage | undefined;
  isObserve: boolean;
}

type $ObserveMapNwwItem = Map<string /** mmid */, $ObserveItem>;

type $ObserveMapNww = Map<
  // nww
  Electron.BrowserWindow,
  $ObserveMapNwwItem
>;
