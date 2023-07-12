import { proxy } from "comlink";
import type { Ipc } from "../../core/ipc/index.ts";
import { $MMID } from "../../core/types.ts";
import { debounceQueueMicrotask } from "../../helper/$debounce.ts";
import { mapHelper } from "../../helper/mapHelper.ts";
import { createNativeWindow } from "../../helper/openNativeWindow.ts";

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
  constructor(readonly win: Electron.BrowserWindow) {
    this.win.on("close", () => {
      for (const item of this._allBrowserView) {
        item.view.webContents.closeDevTools();
      }
    });
  }
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
    view.setBounds({ height, width, x: 0, y: 0 });
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
    if (itemIndex === -1) {
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
