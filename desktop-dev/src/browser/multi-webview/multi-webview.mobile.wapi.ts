import { proxy } from "comlink";
import type { Ipc } from "../../core/ipc/index.ts";
import { debounceQueueMicrotask } from "../../helper/$debounce.ts";
import { mapHelper } from "../../helper/mapHelper.ts";
import { createNativeWindow } from "../../helper/openNativeWindow.ts";
import { $MMID } from "../../helper/types.ts";

// export type $RenderApi = import("./assets/appSheel.api.ts").$Api;

// export type $WApi = { nww: $NativeWindow; apis: Remote<$RenderApi> };
// /**
//  * WARN 这里的 wapis 是全局共享的，也就是说无法实现逻辑上的多实例，务必注意
//  *
//  * 要实现也可以，这里的key需要额外有一个根据 mwebview 的归组逻辑，比如： file://mwebview.browser.dweb/uid 获得一个实例编号
//  */
// export const _mmid_wapis_map = new Map<$MMID, $WApi>();
// export const getAllWapis = () => _mmid_wapis_map.entries();
// export const deleteWapis = (filter: (wapi: $WApi, mmid: $MMID) => boolean) => {
//   for (const [mmid, wapi] of _mmid_wapis_map) {
//     if (filter(wapi, mmid)) {
//       wapi.nww.close();
//       _mmid_wapis_map.delete(mmid);
//     }
//   }
// };
// export const apisGetFromMmid = (mmid: $MMID) => {
//   return _mmid_wapis_map.get(mmid)?.apis;
// };

// export const nwwGetFromMmid = (mmid: $MMID) => {
//   return _mmid_wapis_map.get(mmid)?.nww;
// };

// const init_preload_js = (async () => {
//   const preload_js_path = resolveToDataRoot(
//     `mwebview/${Electron.app.getVersion()}/viewItem.preload.js`
//   );
//   console.log(preload_js_path);
//   /// preload.js 在启动后，进行第一次写入；如果在开发模式下，那么每次启动都强制写入
//   if (isElectronDev || fs.existsSync(preload_js_path) === false) {
//     /// 引入代码，编译后的，然后保存到外部文件中
//     const preloadCode = (
//       await import("./assets/viewItem.preload.ts")
//     ).default.toString();
//     fs.mkdirSync(path.dirname(preload_js_path), { recursive: true });
//     fs.writeFileSync(preload_js_path, `(${preloadCode})()`);
//   }
//   return pathToFileURL(preload_js_path).href;
// })();

// export function forceGetWapis(this: MicroModule, ipc: Ipc, root_url: string) {
//   ipc.onClose(() => {
//     // 是否会出现 一个 JsMicroModule 打开其他的 JsMicroModule
//     // 的情况，如果是这样的话会出现一个 borserWindow 内会包含连个应用
//     // 当前判断不可以 是不可以 一个 browserWindow 内只会以一个 应用
//     const wapi = _mmid_wapis_map.get(ipc.remote.mmid);
//     wapi?.nww.close();
//     _mmid_wapis_map.delete(ipc.remote.mmid);
//   });

//   return locks.request(
//     "multi-webview-get-window-" + ipc.remote.mmid,
//     async () => {
//       let wapi = _mmid_wapis_map.get(ipc.remote.mmid);
//       if (wapi === undefined) {
//         this.nativeFetch(`file://js.browser.dweb/bw?action=show`);
//         const diaplay = Electron.screen.getPrimaryDisplay();
//         const url = new URL(root_url);
//         url.searchParams.set("uid", ipc.uid.toString());
//         const nww = await createComlinkNativeWindow(url.href, {
//           webPreferences: {
//             webviewTag: true,
//           },
//           // transparent: true,
//           // autoHideMenuBar: true,
//           // 测试代码
//           width: 375,
//           height: 800,
//           x: 0,
//           y: (diaplay.size.height - 800) / 2,
//           show: true,
//           // frame: false,
//         });

//         nww.on("close", () => {
//           _mmid_wapis_map.delete(ipc.remote.mmid);
//           if (_mmid_wapis_map.size <= 0) {
//             this.nativeFetch(`file://js.browser.dweb/bw?action=hide`);
//           }
//         });

//         const apis = nww.getApis<$RenderApi>();

//         apis.preloadAbsolutePathSet(await init_preload_js);

//         _mmid_wapis_map.set(ipc.remote.mmid, (wapi = { nww, apis }));
//       }
//       return wapi;
//     }
//   );
// }

export type $MWebviewWindow = Awaited<ReturnType<typeof _createMWebViewWindow>>;

export const ALL_MMID_MWEBVIEW_WINDOW_MAP = new Map<
  $MMID,
  Promise<$MWebviewWindow>
>();
/**
 * 为远端模块创建一个 mwebview-window
 */
export function getOrOpenMWebViewWindow(ipc: Ipc) {
  return mapHelper.getOrPut(
    ALL_MMID_MWEBVIEW_WINDOW_MAP,
    ipc.remote.mmid,
    async () => {
      const mwebviewWindow = await _createMWebViewWindow(ipc);
      mwebviewWindow.win.on("closed", () => {
        ALL_MMID_MWEBVIEW_WINDOW_MAP.delete(ipc.remote.mmid);
      });
      mwebviewWindow.win.focus();
      return mwebviewWindow;
    }
  );
}

export const getMWebViewWindow = (ipc: Ipc) => {
  return ALL_MMID_MWEBVIEW_WINDOW_MAP.get(ipc.remote.mmid);
};

const _createMWebViewWindow = async (ipc: Ipc) => {
  const diaplay = Electron.screen.getPrimaryDisplay();
  const nww = await createNativeWindow(ipc.remote.mmid, {
    // webPreferences: {
    //   webviewTag: true,
    // },
    // transparent: true,
    // autoHideMenuBar: true,
    // frame: false,
    // 测试代码
    width: 375,
    height: 800,
    x: 0,
    y: (diaplay.size.height - 800) / 2,
  });
  const mwebviewApi = new MWebviewController(nww);

  return mwebviewApi;
};

export interface $MWebviewItem {
  view: Electron.BrowserView;
  zIndex: number;
  isVisiable: boolean;
  cornerRadius: number;
}

export class MWebviewController {
  constructor(readonly win: Electron.BrowserWindow) {}
  private _id(subfix: string) {
    return `MWebviewController-${this.win.id}-${subfix}`;
  }

  private _allBrowserView: $MWebviewItem[] = [];
  private _allBrowserViewReOrder() {
    this._allBrowserView
      /// 重新排序一遍
      .sort((a, b) => a.zIndex - b.zIndex)
      .forEach((item, index) => {
        item.zIndex = index + 1;
      });

    /// 更新其它状态
    this._refreshTopBrowserView();
    this._refreshWindowTitle();
  }
  /**
   * 将排序结果反映到视图中
   */
  private _refreshTopBrowserView() {
    debounceQueueMicrotask(this._id("setTopBrowserView"), () => {
      for (const item of this._allBrowserView) {
        this.win.setTopBrowserView(item.view);
      }
    });
  }

  private _preLastItem?: $MWebviewItem;
  private _on_title_change = (event: Electron.Event, title: string) => {
    this.win.setTitle(title);
  };
  /**
   * 追踪顶层browserview的title
   */
  private _refreshWindowTitle() {
    debounceQueueMicrotask("setWindowTitle", () => {
      /// 更新title
      const lastItem = this._allBrowserView.at(-1);
      if (this._preLastItem !== lastItem) {
        this._preLastItem?.view.webContents.off(
          "page-title-updated",
          this._on_title_change
        );
      }

      this._preLastItem = lastItem;
      if (lastItem !== undefined) {
        this.win.setTitle(lastItem.view.webContents.getTitle());
        lastItem.view.webContents.on(
          "page-title-updated",
          this._on_title_change
        );
      }
    });
  }

  getAllBrowserView() {
    return this._allBrowserView.slice();
  }

  createBrowserView(
    url: string,
    options?: Electron.BrowserViewConstructorOptions
  ) {
    const view = new Electron.BrowserView(options);
    this.win.addBrowserView(view);
    this.win.setTopBrowserView(view); // 排在顶层，立刻生效
    this._refreshWindowTitle();

    this._allBrowserView.push({
      view: proxy(view),
      zIndex: this._allBrowserView.length + 1,
      isVisiable: true,
      cornerRadius: 0,
    });
    view.webContents.on("destroyed", () => {
      this.deleteBrowserView(view);
    });

    /// 目前，这个版本的mwebview就是简单的堆叠，所以我们用窗口的大小来铺满视图
    const [width, height] = this.win.getContentSize();
    const [_, height_with_titlebar] = this.win.getSize();
    view.setBounds({ height, width, x: 0, y: height_with_titlebar - height });
    view.setAutoResize({ width: true, height: true });
    view.webContents.openDevTools({ mode: "detach" });
    if (url) {
      view.webContents.loadURL(url);
    }

    return proxy(view);
  }
  deleteBrowserView(view: Electron.BrowserView) {
    const itemIndex = this._allBrowserView.findIndex(
      (item) => item.view === view
    );
    if (!itemIndex) {
      return false;
    }
    this.win.removeBrowserView(view);
    view.webContents.close();

    this._allBrowserView.splice(itemIndex, 1);
    this._allBrowserViewReOrder();

    /// 如果没有视图了，就自动关闭窗口
    debounceQueueMicrotask(this._id("close-window"), () => {
      if (this._allBrowserView.length === 0) {
        this.win.close();
      }
    });
    return true;
  }
  /**
   * 设定一个排序值，但是返回最终的位置序号
   * @param view
   * @param zIndex
   * @returns
   */
  setBrowserViewZIndex(view: Electron.BrowserView, zIndex: number) {
    const item = this._allBrowserView.find((item) => item.view === view);
    if (!item) {
      return -1;
    }

    item.zIndex = zIndex + 0.5; /// 先设置一个虚高的值，确保不和其它同index的冲突
    this._allBrowserViewReOrder();
    return item.zIndex;
  }
  getBrowserViewZIndex(view: Electron.BrowserView) {
    const item = this._allBrowserView.find((item) => item.view === view);
    if (!item) {
      return -1;
    }
    return item.zIndex;
  }
  setTopBrowserView(view: Electron.BrowserView) {
    return this.setBrowserViewZIndex(view, this._allBrowserView.length);
  }
}
