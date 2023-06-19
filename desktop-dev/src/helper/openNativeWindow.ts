import { expose, proxy, wrap } from "comlink";
import { PromiseOut } from "./PromiseOut.ts";
import { animate, easeOut } from "./animate.ts";
import "./electron.ts";

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

export const openNativeWindow = async (
  url: string,
  options: Electron.BrowserWindowConstructorOptions = {},
  webContentsConfig: { userAgent?: (userAgent: string) => string } = {}
) => {
  const { MainPortToRenderPort } = await import("./electronPortMessage.ts");
  await Electron.app.whenReady();

  options.webPreferences = {
    ...options.webPreferences,
    // preload: resolveTo("./openNativeWindow.preload.cjs"),
    sandbox: false,
    devTools: true,
    webSecurity: false,
    nodeIntegration: true,
    contextIsolation: false,
  };

  const win = new Electron.BrowserWindow(options) as $ExtendsBrowserWindow;
  if (webContentsConfig.userAgent) {
    win.webContents.setUserAgent(
      webContentsConfig.userAgent(win.webContents.userAgent)
    );
  }
  win._devToolsWin = new Map();
  const show_po = new PromiseOut<void>();
  win.once("ready-to-show", () => {
    win.show();
    // 是否显示 multi-webview devTools;
    // 这个只是在开发 desktop-dev 的阶段才需要之后是不需要的
    const devToolsWin = openDevToolsAtBrowserWindowByWebContents(
      win.webContents,
      win.webContents.getTitle(),
      win
    );
    devToolsWin.once("ready-to-show", () => {
      win._devToolsWin.set(devToolsWin.webContents.id, devToolsWin);
    });
    show_po.resolve();
  });

  win.on("close", () => {
    const devToolsWin = win._devToolsWin.values();
    const devToolsWinArr = Array.from(devToolsWin);
    devToolsWinArr.forEach((item) => {
      item.close();
      win._devToolsWin.delete(item.webContents.id);
    });
  });
  // Emitted when the window gains focus.
  win.on("focus", () => {
    // 设置匹配的 worker 也进入到 top 层
    // const devToolsWin = win._devToolsWin.values()
    // const devToolsWinArr = Array.from(devToolsWin).reverse();
    // devToolsWinArr.forEach(item => {
    //    item.showInactive()
    // })
    win.show();
  });

  win.webContents.setWindowOpenHandler((_detail) => {
    return { action: "deny" };
  });

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
  await show_po.promise;

  const { import_port, export_port } = await ports_po.promise;

  expose(new ForRenderApi(win), export_port);
  return Object.assign(win, {
    getApis<T>() {
      return wrap<T>(import_port);
    },
  });
};

/**
 * 根据 webContents 打开一个window对象用来承载 devTools
 * @param webContents
 * @param title
 * @param y
 * @returns
 */
function openDevToolsAtBrowserWindowByWebContents(
  _webContents: Electron.WebContents,
  title: string,
  _win: Electron.BrowserWindow
) {
  const [winDispose, win] = eventScope(_win);
  const [webContentsDispose, webContents] = eventScope(_webContents);

  const diaplay = Electron.screen.getPrimaryDisplay();
  const space = 10;
  const winBounds = win.getBounds();
  const devTools = new Electron.BrowserWindow({
    title: title, // 好像没有效果
    autoHideMenuBar: true,
    width: Math.min(
      Math.max(400, diaplay.size.width - winBounds.width - space),
      800
    ),
    height: 800,
    webPreferences: {
      // partition: "devtools",
    },
  });
  devTools.webContents.on("destroyed", () => {
    winDispose();
    webContentsDispose();
  });

  webContents.setDevToolsWebContents(devTools.webContents);
  webContents.openDevTools({ mode: "detach" });

  /// 开发者工具的窗口进行跟随
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
    const newDevPos = {
      x: winBounds.x + space + winBounds.width,
      y: winBounds.y,
    };
    if (oldDevPos.x !== newDevPos.x || oldDevPos.y !== newDevPos.y) {
      preAniAborter?.();
      preAniAborter = undefined;

      if (typeof animate === "boolean") {
        devTools.setPosition(newDevPos.x, newDevPos.y, animate);
        oldDevPos = newDevPos;
      } else {
        preAniAborter = animate(
          oldDevPos,
          newDevPos,
          (pos) => {
            devTools.setPosition(Math.round(pos.x), Math.round(pos.y), false);
            oldDevPos = pos;
          },
          () => {
            preAniAborter = undefined;
          }
        );
      }
    }
  };
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
  win.on("focus", () => devTools.moveTop());
  devTools.on("focus", () => win.moveTop());

  webContents.on("did-start-navigation", () => {
    webContents.setDevToolsWebContents(devTools.webContents);
  });
  devTools.webContents.executeJavaScript(
    `(()=>{
      document.title = ${JSON.stringify(`for: ${title}`)}
    })()`
  );
  return devTools;
}

export class ForRenderApi {
  constructor(private win: $ExtendsBrowserWindow) {}
  // private _devToolsWin: Map<number, Electron.BrowserWindow> = new Map();

  /**
   * 在一个新的窗口打开 devTools
   * @param webContentsId
   * @param src
   */
  openDevToolsAtBrowserWindowByWebContentsId(
    webContentsId: number,
    title: string
  ) {
    const content_wcs = Electron.webContents.fromId(webContentsId)!;
    this.win._devToolsWin.set(
      webContentsId,
      openDevToolsAtBrowserWindowByWebContents(content_wcs, title, this.win)
    );
  }

  /**
   * 根据 devTools window 匹配的 webContentsId
   * 关闭 devTools window
   * 从保存的 列表中删除
   * @param webContentsId
   */
  closeDevToolsAtBrowserWindowByWebContentsId(webContentsId: number) {
    const devToolsWin = this.win._devToolsWin.get(webContentsId);
    if (devToolsWin === undefined) throw new Error(`devToolsWin === undefined`);
    devToolsWin.close();
    this.win._devToolsWin.delete(webContentsId);
  }

  // openDevTools(
  //   webContentsId: number,
  //   options?: Electron.OpenDevToolsOptions,
  //   devToolsId?: number
  // ) {
  //   // 原始代码
  //   const content_wcs = Electron.webContents.fromId(webContentsId);
  //   if (content_wcs === undefined) throw new Error(`content_wcs === undefined`);
  //   if (devToolsId) {
  //     const devTools_wcs = Electron.webContents.fromId(devToolsId);
  //     if (devTools_wcs === undefined)
  //       throw new Error(`content_wcs === undefined`);
  //     content_wcs.setDevToolsWebContents(devTools_wcs);
  //     queueMicrotask(() => {
  //       devTools_wcs.executeJavaScript("window.location.reload()");
  //     });
  //   }
  //   content_wcs.openDevTools(options);
  // }

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
    const devToolsWin = this.win._devToolsWin.get(webContentsId);
    if (devToolsWin === undefined) throw new Error(`devToolsWin === undefined`);
    devToolsWin.close();
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

export interface $ExtendsBrowserWindow extends Electron.BrowserWindow {
  _devToolsWin: Map<number, Electron.BrowserWindow>;
}
// export interface $DevToolsWin {
//   _devToolsWin: Map<number, Electron.BrowserWindow>
// }
