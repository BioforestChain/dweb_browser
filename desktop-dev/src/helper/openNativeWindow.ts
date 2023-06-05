import { expose, proxy, wrap } from "comlink";
import { PromiseOut } from "./PromiseOut.ts";
import "./electron.ts";

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
      0
    );
    devToolsWin.once("ready-to-show", () => {
      win._devToolsWin.set(devToolsWin.webContents.id, devToolsWin)
    });
    show_po.resolve();
  });
  
  win.on("close", () => {
    const devToolsWin = win._devToolsWin.values()
    const devToolsWinArr = Array.from(devToolsWin);
    devToolsWinArr.forEach(item => {
      item.close();
      win._devToolsWin.delete(item.webContents.id)
    })
  })

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
  webContents: Electron.WebContents,
  title: string,
  y?: number
) {
  const diaplay = Electron.screen.getPrimaryDisplay();
  const space = 10;
  const devTools = new Electron.BrowserWindow({
    title: title, // 好像没有效果
    autoHideMenuBar: true,
    width: diaplay.size.width - 375 - space,
    height: 800,
    x: 375 + space,
    y: y !== undefined ? y : (diaplay.size.height - 800) / 2,
    webPreferences: {
      partition: "devtools",
    },
  });
  webContents.setDevToolsWebContents(devTools.webContents);
  webContents.openDevTools({mode: "detach"});
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
      openDevToolsAtBrowserWindowByWebContents(content_wcs, title)
    );
  }

  openDevTools(
    webContentsId: number,
    options?: Electron.OpenDevToolsOptions,
    devToolsId?: number
  ) {
    // 原始代码
    const content_wcs = Electron.webContents.fromId(webContentsId);
    if (content_wcs === undefined) throw new Error(`content_wcs === undefined`);
    if (devToolsId) {
      const devTools_wcs = Electron.webContents.fromId(devToolsId);
      if (devTools_wcs === undefined)
        throw new Error(`content_wcs === undefined`);
      content_wcs.setDevToolsWebContents(devTools_wcs);
      queueMicrotask(() => {
        devTools_wcs.executeJavaScript("window.location.reload()");
      });
    }
    content_wcs.openDevTools(options);
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

export interface $ExtendsBrowserWindow extends Electron.BrowserWindow{
  _devToolsWin: Map<number, Electron.BrowserWindow>
}
// export interface $DevToolsWin {
//   _devToolsWin: Map<number, Electron.BrowserWindow>
// }
