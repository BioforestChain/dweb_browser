import type { OutgoingMessage } from "node:http";
import type { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { $Schema1ToType } from "../../core/helper/types.ts";
import { Ipc, IpcEvent, IpcRequest } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
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
    return view.webContents.id;
  }

  protected override async _shutdown() {
    for (const [mmid, mwwP] of ALL_MMID_MWEBVIEW_WINDOW_MAP) {
      const mww = await mwwP;
      mww.win.close();
    }
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
