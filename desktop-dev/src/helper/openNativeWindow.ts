import { expose, proxy, wrap,releaseProxy } from "comlink";
import { debounce } from "./$debounce.ts";
import { PromiseOut } from "./PromiseOut.ts";
import { animate, easeOut } from "./animate.ts";
import "./electron.ts";
import { electronConfig } from "./electronConfig.ts";

/**
 * 事件绑定作用域
 * 返回一个销毁器，使得方便对于作用域内绑定的事件进行集体销毁
 * @param target
 * @returns
 */
function eventScope<T extends { on: Function; once: Function; off: Function }>(
  target: T
) {
  const onList: {
    name: string;
    handler: any;
  }[] = [];
  const dispose = () => {
    for (const info of onList) {
      target.off(info.name, info.handler);
    }
  };
  const proxyer = new Proxy(target, {
    get(target, prop, receiver) {
      if (prop === "on") {
        return (name: string, handler: any) => {
          onList.push({ name, handler });
          return target.on(name, handler);
        };
      } else if (prop === "once") {
        return (name: string, handler: any) => {
          onList.push({ name, handler });
          return target.once(name, handler);
        };
      }
      let res = Reflect.get(target, prop, receiver);
      if (typeof res === "function") {
        res = res.bind(target);
      }
      return res;
    },
  });

  return [dispose, proxyer] as const;
}

const DEVTOOLS_STATE = "native-window-states";
declare global {
  interface ElectronConfig {
    [DEVTOOLS_STATE]?: {
      [url: string]: {
        devtools: boolean;
        bounds: Electron.Rectangle;
      };
    };
  }
}
const nativeWindowStates = electronConfig.get(DEVTOOLS_STATE, {});
const saveNativeWindowStates = () => {
  electronConfig.set(DEVTOOLS_STATE, nativeWindowStates);
};

/**
 * 打开 BrowserWindow。
 * 这里会记录窗口的大小、位置
 * 会记录开发者工具是否打开
 *
 * 会提供一个基于comlink的双工通讯
 * PS：前提是代码里头必须注入 openNaiveWindow.preload.ts
 * 我们不会使用preload字段，请开发者通过代码导入的方式进行编译！
 * ```ts
 * if ("ipcRenderer" in self) {
 *   (async () => {
 *     const { exportApis } = await import(
 *       "~/helper/openNativeWindow.preload.ts"
 *     );
 *     exportApis(globalThis);
 *   })();
 * }
 * ```
 *
 * @param url
 * @param _options
 * @param webContentsConfig
 * @returns
 */
export const openNativeWindow = async (
  url: string,
  _options: Electron.BrowserWindowConstructorOptions = {},
  webContentsConfig: { userAgent?: (userAgent: string) => string } = {}
) => {
  const { MainPortToRenderPort } = await import("./electronPortMessage.ts");
  await Electron.app.whenReady();

  const options: Electron.BrowserWindowConstructorOptions = {
    ..._options,
    webPreferences: {
      ..._options.webPreferences,
      sandbox: false,
      devTools: !Electron.app.isPackaged,
      webSecurity: false,
      nodeIntegration: true,
      contextIsolation: false,
    },
    ...nativeWindowStates[url]?.bounds,
  };

  const win = new Electron.BrowserWindow(options);

  const state = (nativeWindowStates[url] ??= {
    devtools: false,
    bounds: win.getBounds(),
  });

  if (webContentsConfig.userAgent) {
    win.webContents.setUserAgent(
      webContentsConfig.userAgent(win.webContents.userAgent)
    );
  }
  /// 在开发模式下，显示 mwebview 的开发者工具
  if (!Electron.app.isPackaged) {
    if (state.devtools === true) {
      win.webContents.openDevTools();
    }
    win.webContents.on("devtools-opened", () => {
      state.devtools = true;
      saveNativeWindowStates();
    });
    win.webContents.on("devtools-closed", () => {
      state.devtools = false;
      saveNativeWindowStates();
    });
  }

  win.on("close", () => {
    state.bounds = win.getBounds();
    saveNativeWindowStates();
  });

  // win.webContents.setWindowOpenHandler((_detail) => {
  //   return { action: "deny" };
  // });

  const ports_po = new PromiseOut<{
    import_port: MessagePort;
    export_port: MessagePort;
  }>();

  win.webContents.ipc.once("renderPort", (event) => {
    const [import_port, export_port] = event.ports;

    ports_po.resolve({
      import_port: MainPortToRenderPort(import_port),
      export_port: MainPortToRenderPort(export_port),
    });
  });

  await win.loadURL(url);

  const { import_port, export_port } = await ports_po.promise;

  expose(new ForRenderApi(win), export_port);
  return Object.assign(win, {
    getApis<T>() {
      return wrap<T>(import_port);
    },
  });
};

/**
 * 根据 webContents 打开一个窗口对象用来承载 devTools，该窗口会跟随原有 webContents 所在的窗口
 * 这个方法是给 webview-tag 使用的，否则如果是BrowserView，请直接使用原生的开发者工具模式
 * @param webContents
 * @param title
 * @param y
 * @returns
 */
async function openDevToolsWindowAsFollower(
  _webContents: Electron.WebContents,
  followOptions: {
    position?: "left" | "right";
    /**
     * 跟随留白
     */
    space?: number;
    devWebContent?: Electron.WebContents;
  } = {}
) {
  try {
    if (_webContents.devToolsWebContents) {
      const devWin = Electron.BrowserWindow.fromWebContents(
        _webContents.devToolsWebContents
      )!;
      devWin.moveTop();
      return devWin;
    }
  } catch {}

  /// 使用最原始的方式打开webview，放在独立的窗口中
  const devWinPo = new PromiseOut<Electron.BrowserWindow>();
  {
    const { devWebContent } = followOptions;
    /// 如果传入了自定义的devWebContent，那么使用传入的
    if (devWebContent) {
      _webContents.setDevToolsWebContents(devWebContent);
    }
    /// 否则创建一个新的窗口来承载
    else {
      const devWin = new Electron.BrowserWindow();
      _webContents.setDevToolsWebContents(devWin.webContents);
    }
    /// 附加开发窗口
    _webContents.once("devtools-opened", () => {
      const devToolsWebContents = _webContents.devToolsWebContents!;
      devWinPo.resolve(
        Electron.BrowserWindow.fromWebContents(devToolsWebContents)!
      );
    });
    _webContents.openDevTools({ mode: "detach" });
  }

  const _win = Electron.BrowserWindow.fromWebContents(_webContents)!;
  const [winDispose, win] = eventScope(_win);
  const [webContentsDispose, webContents] = eventScope(_webContents);

  const { space = 10 } = followOptions;

  const _devWin = await devWinPo.promise;
  const [devWinDispose, devWin] = eventScope(_devWin);

  /// 调整窗口大小
  {
    const [width, height] = _devWin.getSize();
    const diaplay = Electron.screen.getPrimaryDisplay();
    const winBounds = win.getBounds();
    _devWin.setSize(
      Math.min(
        Math.max(400, width),
        diaplay.size.width - winBounds.width - space
      ),
      Math.min(Math.max(winBounds.height, height), diaplay.size.height)
    );
  }

  /// 绑定销毁关系
  /// devTools 关闭时，解除绑定
  _devWin.webContents.on("destroyed", () => {
    /// 这里不需要调用 _webContents.closeDevTools，因为对于 setDevToolsWebContents 模式，_webContents.closeDevTools 是不会生效的
    /// 当前的 devWin.webContents 本质就是 _webContents.devToolsWebContents，所以随着它被销毁， _webContents.devToolsWebContents 也就成 null了
    winDispose();
    webContentsDispose();
    devWinDispose();
  });
  /// web 销毁时，关闭 devTools
  webContents.on("destroyed", () => {
    /// 关闭 devToolsWebContents，主要，不是 devWin.close
    /// 如果是独立的BrowserWindow，那么等价于 devWin.close
    /// 如果是 webview-tag 或者 BrowserView，那么由调用者自己去处理视图的销毁，同时 devWin 并不会被关闭
    _devWin.webContents.close();
  });

  /// 开发者工具的窗口进行跟随
  const { position: followPosition = "right" } = followOptions;
  let oldDevPos = { x: -Infinity, y: -Infinity };
  let preAniAborter: undefined | (() => void);
  const devWinFollow = (
    animate:
      | boolean
      | ((
          from: Electron.Point,
          to: Electron.Point,
          onUpdate: (pos: Electron.Point) => void,
          onComplete?: () => void
        ) => () => void) = false
  ) => {
    const winBounds = win.getBounds();
    let newDevPos: Electron.Point;
    if (followPosition === "left") {
      newDevPos = {
        x: winBounds.x - space - _devWin.getSize()[0],
        y: winBounds.y,
      };
    } else {
      /// followPosition === "right"
      newDevPos = {
        x: winBounds.x + space + winBounds.width,
        y: winBounds.y,
      };
    }
    if (oldDevPos.x !== newDevPos.x || oldDevPos.y !== newDevPos.y) {
      preAniAborter?.();
      preAniAborter = undefined;

      if (typeof animate === "boolean") {
        _devWin.setPosition(newDevPos.x, newDevPos.y, animate);
        oldDevPos = newDevPos;
      } else {
        preAniAborter = animate(
          oldDevPos,
          newDevPos,
          (pos) => {
            _devWin.setPosition(Math.round(pos.x), Math.round(pos.y), false);
            oldDevPos = pos;
          },
          () => {
            preAniAborter = undefined;
          }
        );
      }
    }
  };
  /// 只要不是同一个窗口，那么就可以使用跟随模式
  if (_devWin !== _win) {
    devWin.on("moved", () => {
      const [x, y] = _devWin.getPosition();
      oldDevPos.x = x;
      oldDevPos.y = y;
    });
    const debounceDevWinFollow = debounce(devWinFollow, 300);
    devWin.on("resize", () => debounceDevWinFollow(true));
    win.on("resize", () => devWinFollow());
    /// TODO moved MACOS only
    win.on("moved", () =>
      devWinFollow((from, to, onUpdate, onComplete) => {
        const ani = animate({
          ease: easeOut,
          from,
          to,
          onUpdate: onUpdate,
          onComplete: onComplete,
        });
        return () => {
          ani.stop();
        };
      })
    );
    devWinFollow(true);
    win.on("focus", () => _devWin.moveTop());
    _devWin.on("focus", () => win.moveTop());
  }

  return _devWin;
}

export class ForRenderApi {
  constructor(private win: Electron.BrowserWindow) {}
  // private _devToolsWin: Map<number, Electron.BrowserWindow> = new Map();

  /**
   * 在一个新的窗口打开 devTools
   * @param webContentsId
   * @param src
   */
  async openDevToolsWindowAsFollower(webContentsId: number, title?: string) {
    const content_wcs = Electron.webContents.fromId(webContentsId)!;
    const devWin = await openDevToolsWindowAsFollower(content_wcs, {});
    if (title !== undefined) {
      devWin.setTitle(title);
    }
    return proxy(devWin);
  }

  /**
   * 标准方法打开 devTools
   * @param webContentsId
   * @param options
   */
  async openDevTools(
    webContentsId: number,
    options?: Electron.OpenDevToolsOptions
  ) {
    const content_wcs = Electron.webContents.fromId(webContentsId)!;
    content_wcs.openDevTools(options);
  }

  async isFullScreen() {
    return this.win.isFullScreen();
  }
  async isFullScreenable() {
    return this.win.isFullScreenable();
  }
  async setFullScreen(v: boolean) {
    return (this.win.fullScreen = v);
  }

  async maximize() {
    this.win.maximize();
  }
  async isMaximized() {
    return this.win.isMaximized();
  }
  async isMaximizable() {
    return this.win.isMaximizable();
  }

  async minimize() {
    this.win.minimize();
  }
  async isMinimized() {
    return this.win.isMinimized();
  }
  async isMinimizable() {
    return this.win.isMinimizable();
  }

  private allBrowserView: { view: Electron.BrowserView; zIndex: number }[] = [];
  private _allBrowserViewReOrder() {
    this.allBrowserView
      /// 重新排序一遍
      .sort((a, b) => a.zIndex - b.zIndex)
      .forEach((item, index) => {
        item.zIndex = index + 1;
        /// 将排序结果反映到视图中
        this.win.setTopBrowserView(item.view);
      });
  }

  async createBrowserView(options?: Electron.BrowserViewConstructorOptions) {
    const view = new Electron.BrowserView(options);
    this.win.addBrowserView(view);
    this.win.setTopBrowserView(view);
    const item = { view: proxy(view), zIndex: this.allBrowserView.length + 1 };
    this.allBrowserView.push(item);
    return proxy(view);
  }
  async deleteBrowserView(view: Electron.BrowserView) {
    const itemIndex = this.allBrowserView.findIndex(
      (item) => item.view === view
    );
    if (!itemIndex) {
      return false;
    }
    this.win.removeBrowserView(view);
    view.webContents.close();

    this.allBrowserView.splice(itemIndex, 1);
    this._allBrowserViewReOrder();
    return true;
  }
  /**
   * 设定一个排序值，但是返回最终的位置序号
   * @param view
   * @param zIndex
   * @returns
   */
  async setBrowserViewZIndex(view: Electron.BrowserView, zIndex: number) {
    const item = this.allBrowserView.find((item) => item.view === view);
    if (!item) {
      return -1;
    }

    item.zIndex = zIndex + 0.5; /// 先设置一个虚高的值，确保不和其它同index的冲突
    this._allBrowserViewReOrder();
    return item.zIndex;
  }
  async getBrowserViewZIndex(view: Electron.BrowserView) {
    const item = this.allBrowserView.find((item) => item.view === view);
    if (!item) {
      return -1;
    }
    return item.zIndex;
  }
  async setTopBrowserView(view: Electron.BrowserView) {
    return this.setBrowserViewZIndex(view, this.allBrowserView.length);
  }

  /**
   * 关闭 devTools
   * @param webContentsId
   */
  async closeDevTools(webContentsId: number) {
    const content_wcs = Electron.webContents.fromId(webContentsId)!;
    if (!content_wcs.devToolsWebContents) {
      return;
    }
    const devWin = Electron.BrowserWindow.fromWebContents(
      content_wcs.devToolsWebContents
    );
    /// 如果这个devTools被附加到其它窗口，那么就直接关闭那个窗口
    if (devWin !== this.win) {
      devWin?.close();
    } else {
      /// 如果是与当前窗口使用同一个具柄，那么调用原始的关闭即可
      content_wcs.closeDevTools();
    }
  }

  denyWindowOpenHandler(
    webContentsId: number,
    onDeny: (details: Electron.HandlerDetails) => unknown
  ) {
    const contents = Electron.webContents.fromId(webContentsId);
    if (contents === undefined) throw new Error(`contents === undefined`);
    return contents.setWindowOpenHandler((detail) => {
      onDeny(detail);
      return { action: "deny" };
    });
  }

  destroy(webContentsId: number, options?: Electron.CloseOpts) {
    const contents = Electron.webContents.fromId(webContentsId);
    if (contents === undefined) throw new Error(`contents === undefined`);
    // const devToolsWin = this.win._devToolsWin.get(webContentsId);
    // if (devToolsWin === undefined) throw new Error(`devToolsWin === undefined`);
    // devToolsWin.close();
    return contents.close(options);
  }

  onDestroy(webContentsId: number, onDestroy: () => unknown) {
    const contents = Electron.webContents.fromId(webContentsId);
    if (contents === undefined) throw new Error(`contents === undefined`);
    contents.addListener("destroyed", () => {
      onDestroy();
    });
  }

  getWenContents(webContentsId: number) {
    const contents = Electron.webContents.fromId(webContentsId);
    if (contents === undefined) throw new Error(`contents === undefined`);
    return proxy(contents);
  }

  // 关闭 Browserwindow
  closedBrowserWindow() {
    this.win.close();
  }
}

// export const wrapNativeWindowAsApis = <T>(win: $NativeWindow) => {
//   wrap({
//     addEventListener(type, listener: (...args: any[]) => void) {
//       win.webContents.ipc.on(type, listener);
//     },
//     removeEventListener(type, listener: (...args: any[]) => void) {
//       win.webContents.ipc.off(type, listener);
//     },
//     postMessage(messaeg, transfer) {
//       win.webContents.ipc.;
//     },
//   }) as T;
// };

export type $NativeWindow = Awaited<ReturnType<typeof openNativeWindow>>;

// {
//   readonly devToolsWins = new Set<Electron.BrowserWindow>();
// }
// export interface $DevToolsWin {
//   _devToolsWin: Map<number, Electron.BrowserWindow>
// }
