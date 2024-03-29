import { expose, proxy, wrap } from "comlink";
import { debounce } from "./$debounce.ts";
import { PromiseOut } from "./PromiseOut.ts";
import { animate, easeOut } from "./animate.ts";
import "./electron.ts";
import { micaElectron } from "./electron.ts";
import { electronConfig } from "./electronStore.ts";

/**
 * 事件绑定作用域
 * 返回一个销毁器，使得方便对于作用域内绑定的事件进行集体销毁
 * @param target
 * @returns
 */
function eventScope<T extends { on: Function; once: Function; off: Function }>(target: T) {
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
    [DEVTOOLS_STATE]: {
      [url: string]: {
        devtools: boolean;
        bounds: Electron.Rectangle;
      };
    };
  }
}
const nativeWindowStates = electronConfig.get(DEVTOOLS_STATE, () => ({}));
const saveNativeWindowStates = () => {
  electronConfig.set(DEVTOOLS_STATE, nativeWindowStates);
};

export interface $CreateNativeWindowOptions extends Electron.BrowserWindowConstructorOptions {
  userAgent?: (userAgent: string) => string;
  defaultBounds?: Partial<Electron.Rectangle>;
}

export const createNativeWindow = async (sessionId: string, createOptions: $CreateNativeWindowOptions = {}) => {
  await Electron.app.whenReady();
  const { userAgent, defaultBounds, ..._options } = createOptions;

  const options: Electron.BrowserWindowConstructorOptions = {
    ...defaultBounds,
    webPreferences: {
      ..._options.webPreferences,
      sandbox: false,
      devTools: true,
      webSecurity: false,
      nodeIntegration: true,
      contextIsolation: false,
      experimentalFeatures: true,
    },
    ...nativeWindowStates[sessionId]?.bounds,
    ..._options,
  };

  let win: Electron.BrowserWindow;

  if (options.backgroundMaterial) {
    const { backgroundMaterial, ...miac_options } = options;
    const mica_win = new micaElectron.MicaBrowserWindow(miac_options);

    /// 云母特性只能在有边框的情况下生效
    if (miac_options.frame !== false && micaElectron.IS_WINDOWS_11) {
      if (backgroundMaterial === "mica") {
        mica_win.setMicaEffect(); // Mica Effect
        mica_win.enableMargin();
      } else if (backgroundMaterial === "acrylic") {
        mica_win.setMicaAcrylicEffect();
      } else if (backgroundMaterial === "tabbed") {
        mica_win.setMicaTabbedEffect(); // Mica Tabbed
      }
    } else {
      // 如果无边框，降级使用 win7+ 的亚克力特效
      if (backgroundMaterial === "mica" || backgroundMaterial === "acrylic" || backgroundMaterial === "tabbed") {
        mica_win.setAcrylic();
      }
    }

    /// 设置圆角，与macos保持一致的效果
    if (micaElectron.IS_WINDOWS_11 && options.roundedCorners) {
      mica_win.setRoundedCorner();
    }

    win = mica_win;
  } else {
    win = new Electron.BrowserWindow(options);
  }

  const state = (nativeWindowStates[sessionId] ??= {
    devtools: false,
    bounds: win.getBounds(),
  });

  if (userAgent) {
    win.webContents.setUserAgent(userAgent(`${win.webContents.userAgent} Dweb/${win.id} (desktop;dweb;)`));
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

  const saveBounds = () => {
    state.bounds = win.getBounds();
    saveNativeWindowStates();
  };
  win.on("moved", debounce(saveBounds, 300));
  win.on("close", () => {
    saveBounds();

    win.webContents.closeDevTools();
  });
  return win;
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
export const createComlinkNativeWindow = async <E = NativeWindowExtensions_BaseApi>(
  url: string,
  createOptions?: $CreateNativeWindowOptions,
  exportBuilder = async (win: Electron.BrowserWindow) => {
    return new NativeWindowExtensions_BaseApi(win) as any as E;
  }
) => {
  const win = await createNativeWindow(new URL(url).host, createOptions);
  const mainApi = await exportBuilder(win);

  return Object.assign(win, await bridgeComlink(url, win.webContents, mainApi));
};

const bridgeComlink = async <M>(url: string, webContents: Electron.WebContents, mainApi: M) => {
  let ports_po: PromiseOut<{
    import_port: MessagePort;
    export_port: MessagePort;
  }>;
  const init_ports_po = () => {
    ports_po = new PromiseOut();
    ports_po.onSuccess((ports) => {
      expose(mainApi, ports.export_port);
    });
    return ports_po;
  };
  ports_po = init_ports_po();
  const { MainPortToRenderPort } = await import("./electronPortMessage.ts");
  let initResolve: { (value: unknown): void };
  const initPromise = new Promise((resolve) => (initResolve = resolve));
  webContents.ipc.on("renderPort", (event) => {
    const [import_port, export_port] = event.ports;
    /// 页面可能刷新，那么就重新初始化
    if (ports_po.is_resolved) {
      ports_po = init_ports_po();
    }
    ports_po.resolve({
      import_port: MainPortToRenderPort(import_port),
      export_port: MainPortToRenderPort(export_port),
    });

    initResolve(ports_po);
  });

  await webContents.loadURL(url);
  await initPromise;
  return {
    getMainApi() {
      return mainApi;
    },
    getRenderApi<T>() {
      return wrap<T>(ports_po.value!.import_port);
    },
  };
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
      const devWin = Electron.BrowserWindow.fromWebContents(_webContents.devToolsWebContents)!;
      devWin.moveTop();
      return devWin;
    }
    // deno-lint-ignore no-empty
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
      devWinPo.resolve(Electron.BrowserWindow.fromWebContents(devToolsWebContents)!);
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
      Math.min(Math.max(400, width), diaplay.size.width - winBounds.width - space),
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

export class NativeWindowExtensions_BaseApi {
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
  async openDevTools(webContentsId: number, options?: Electron.OpenDevToolsOptions) {
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

  /**
   * 关闭 devTools
   * @param webContentsId
   */
  async closeDevTools(webContentsId: number) {
    const content_wcs = Electron.webContents.fromId(webContentsId)!;
    if (!content_wcs.devToolsWebContents) {
      return;
    }
    const devWin = Electron.BrowserWindow.fromWebContents(content_wcs.devToolsWebContents);
    /// 如果这个devTools被附加到其它窗口，那么就直接关闭那个窗口
    if (devWin !== this.win) {
      devWin?.close();
    } else {
      /// 如果是与当前窗口使用同一个具柄，那么调用原始的关闭即可
      content_wcs.closeDevTools();
    }
  }

  denyWindowOpenHandler(webContentsId: number, onDeny: (details: Electron.HandlerDetails) => unknown) {
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


export type $NativeWindow = Awaited<ReturnType<typeof createComlinkNativeWindow>>;

