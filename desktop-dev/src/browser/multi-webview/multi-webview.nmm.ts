import type { OutgoingMessage } from "node:http";
import type { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { Ipc } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { fetchMatch } from "../../helper/patternHelper.ts";
import { zq } from "../../helper/zodHelper.ts";
import { ALL_MMID_MWEBVIEW_WINDOW_MAP, getMWebViewWindow, getOrOpenMWebViewWindow } from "./MWebviewController.ts";

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
    const query_url = zq.object({ url: zq.url() });
    const query_host = zq.object({ host: zq.string() });
    const onFetchHanlder = fetchMatch()
      // 打开窗口
      .get("/open", async (event) => {
        const { url } = query_url(event.searchParams);
        return Response.json(await this._open(url, event.ipc));
      })
      // 激活窗口
      .get("/activate", async (event) => {
        const wmm = await getMWebViewWindow(event.ipc);
        let success = false;
        if (wmm !== undefined) {
          wmm.win.focus();
          success = true;
        }
        return Response.json(success);
      })
      /**
       * 关闭当前激活的window
       * ps: 在桌面端中，每个app的前端页面承载的对象是window,在android则是activity，
       * 每个app都有个单独的service,我们承载在worker里面，这里是只关闭app的前端页面，也即关闭window
       * 每个app只能关闭自己的window
       */
      .get("/close/app", async (event) => {
        const wmm = await getMWebViewWindow(event.ipc);
        let success = false;
        if (wmm !== undefined) {
          wmm.win.close();
          success = true;
        }
        return Response.json(success);
      })
      // 销毁指定的 webview
      .get("/close", async (event) => {
        const mww = await getMWebViewWindow(event.ipc);
        let changed = false;
        if (mww) {
          const { host } = query_host(event.searchParams);
          for (const viewItem of mww.getAllBrowserView()) {
            const url = new URL(viewItem.view.webContents.getURL());
            if (url.host === host) {
              changed = mww.deleteBrowserView(viewItem.view);
            }
          }
        }
        return Response.json(changed);
      });

    this.onFetch(onFetchHanlder.run).internalServerError();
  }

  private _all_open_ipc = new Map<number, Ipc>();
  /**
   * 打开 应用
   * 如果 是由 jsProcess 调用 会在当前的 browserWindow 打开一个新的 webview
   * 如果 是由 NMM 调用的 会打开一个新的 borserWindow 同时打开一个新的 webview
   */
  private async _open(url: string, clientIpc: Ipc) {
    const mww = await getOrOpenMWebViewWindow(clientIpc);
    const view = mww.createBrowserView(url);
    console.log("", "_open", url);
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
