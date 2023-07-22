import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { once } from "../../helper/$once.ts";
import { ChangeableMap } from "../../helper/ChangeableMap.ts";
import { isElectronDev } from "../../helper/electronIsDev.ts";
import { createComlinkNativeWindow, createNativeWindow } from "../../helper/openNativeWindow.ts";
import { fetchMatch } from "../../helper/patternHelper.ts";
import { buildUrl } from "../../helper/urlHelper.ts";
import { zq } from "../../helper/zodHelper.ts";
import { HttpDwebServer, createHttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import { JMM_DB } from "../jmm/jmm.api.serve.ts";
import { $AppMetaData } from "../jmm/jmm.ts";
import { $MMID, Ipc, IpcEvent } from "../js-process/std-dweb-core.ts";
import { window_options } from "./const.ts";
import { desktopStore } from "./desktop.store.ts";

export interface $DesktopAppMetaData extends $AppMetaData {
  running: boolean;
}

export class DesktopNMM extends NativeMicroModule {
  mmid = "desktop.browser.dweb" as const;

  protected async _bootstrap(context: $BootstrapContext) {
    const query_app_id = zq.object({
      app_id: zq.mmid(),
    });
    const query_limit = zq.object({
      limit: zq.number().optional(),
    });

    const taskbarAppList = [...desktopStore.get("taskbar/apps", () => new Set())].reverse();
    const runingApps = new ChangeableMap<$MMID, Ipc>();
    runingApps.onChange((map) => {
      for (const app_id of map.keys()) {
        taskbarAppList.unshift(app_id);
      }
      desktopStore.set("taskbar/apps", new Set(taskbarAppList));
    });

    const onFetchHanlder = fetchMatch()
      .get("/appsInfo", async () => {
        return Response.json(
          (await JMM_DB.all()).map((metaData) => {
            return { ...metaData, running: runingApps.has(metaData.id) };
          })
        );
      })
      .get("/openAppOrActivate", async (event) => {
        const { app_id } = query_app_id(event.searchParams);
        console.always("activity", app_id);
        let ipc = runingApps.get(app_id);

        ipc = await this.connect(app_id);
        if (ipc !== undefined) {
          ipc.postMessage(IpcEvent.fromText("activity", ""));
          /// 如果成功打开，将它“追加”到列表中
          runingApps.delete(app_id);
          runingApps.set(app_id, ipc);
          /// 如果应用关闭，将它从列表中移除
          ipc.onClose(() => {
            runingApps.delete(app_id);
          });
        }

        return Response.json(ipc !== undefined);
      })
      .get("/closeApp", async (event) => {
        const { app_id } = query_app_id(event.searchParams);
        let closed = false;
        if (runingApps.has(app_id)) {
          closed = await context.dns.close(app_id);
          if (closed) {
            runingApps.delete(app_id);
          }
        }
        return Response.json(closed);
      })
      .get("/taskbar/appsInfo", async (event) => {
        const { limit = Infinity } = query_limit(event.searchParams);
        const apps: $DesktopAppMetaData[] = [];

        for (const app_id of taskbarAppList) {
          const metaData = await JMM_DB.find(app_id);
          if (metaData) {
            apps.push({ ...metaData, running: runingApps.has(app_id) });
          }
        }
      });
    this.onFetch(onFetchHanlder.run).internalServerError();

    const taskbarServer = await this._createTaskbarWebServer();
    const desktopServer = await this._createDesktopWebServer();

    const taskbarWin = await this._createTaskbarView(taskbarServer, desktopServer);
  }

  private async _createTaskbarWebServer() {
    const taskbarServer = await createHttpDwebServer(this, {
      subdomain: "taskbar",
      port: 433,
    });
    {
      (await taskbarServer.listen()).onFetch((event) => {
        const { pathname, search } = event.url;
        const url = `file:///sys/browser/desktop/${pathname}?mode=stream`;
        return this.nativeFetch(url);
      });
    }
    return taskbarServer;
  }
  private async _createDesktopWebServer() {
    const desktopServer = await createHttpDwebServer(this, {
      subdomain: "desktop",
      port: 433,
    });
    {
      const API_PREFIX = "/api/";
      (await desktopServer.listen()).onFetch(async (event) => {
        const { pathname, search } = event.url;
        let url: string;
        if (pathname.startsWith(API_PREFIX)) {
          url = `file://${pathname.slice(API_PREFIX.length)}${search}`;
        } else {
          url = `file:///sys/browser/newtab${pathname}?mode=stream`;
        }
        const res = await this.nativeFetch(url);
        return res;
      });
    }
    return desktopServer;
  }

  private async _createTaskbarView(taskbarServer: HttpDwebServer, desktopServer: HttpDwebServer) {
    const taskbarWin = await createComlinkNativeWindow(
      await tryDevUrl(
        taskbarServer.startResult.urlInfo.buildInternalUrl((url) => {
          url.pathname = "/taskbar/index.html";
        }).href,
        `http://localhost:3700/taskbar/index.html`
      ),
      window_options,
      async (win) => {
        return new TaskbarMainApis(this, win, taskbarServer, desktopServer);
      }
    );

    taskbarWin.webContents.openDevTools({ mode: "undocked" });

    // taskbarWin.setPosition()

    return taskbarWin;
  }

  protected _shutdown() {}
}

export class TaskbarMainApis {
  constructor(
    private mm: DesktopNMM,
    private win: Electron.BrowserWindow,
    private taskbarServer: HttpDwebServer,
    private desktopServer: HttpDwebServer
  ) {}
  resize(width: number, height: number) {
    // TODO 贴右边
    const display = Electron.screen.getPrimaryDisplay();
    // const bounds = {
    //   ...this.win.getBounds(),
    //   width,
    //   height,
    // }
    this.win.setBounds({
      ...this.win.getBounds(),
      width,
      height,
    });
    // this.win.title
    // this.win.setTitleBarOverlay({ height: 0 });
  }
  setVibrancy(type: Parameters<Electron.BrowserWindow["setVibrancy"]>[0]) {
    this.win.setVibrancy(type);
  }
  setBackgroundMaterial(material: Parameters<Electron.BrowserWindow["setBackgroundMaterial"]>[0]) {
    this.win.setBackgroundMaterial(material);
  }
  setBackgroundColor(backgroundColor: string) {
    this.win.setBackgroundColor(backgroundColor);
  }
  async openDesktopView() {
    const taskbarWinBounds = this.win.getBounds();

    const desktopWin = await this._createDesktopView(taskbarWinBounds);
    /// TODO 这里isOnTop很难正确检测，因此正确的做法是把desktop-view直接集成到taskbar窗口中
    if (desktopWin.isOnTop()) {
      desktopWin.hide();
    } else {
      /// 获取对应的屏幕
      const display = Electron.screen.getDisplayNearestPoint(taskbarWinBounds);
      /// 我们默认将桌面显示在taskbar左上角
      let desktopWidth = 0;
      let desktopHeight = 0;
      let desktopX = 0;
      let desktopY = 0;
      {
        const uGap = taskbarWinBounds.width / 3;
        const uSize = 1;
        const { width, height } = display.workAreaSize;
        const min_column = uGap * 12;
        const max_column = Math.floor((taskbarWinBounds.x - taskbarWinBounds.width * 0.5) / uSize);
        const min_row = Math.floor(taskbarWinBounds.height / uSize);
        const max_row = Math.floor((taskbarWinBounds.y + taskbarWinBounds.height) / uSize);
        const rec_column = Math.floor((max_column * 2) / 3);
        const rec_row = Math.floor((max_row * 2) / 3);

        const column = Math.min(Math.max(min_column, rec_column), max_column);
        const row = Math.max(Math.min(max_row, rec_row), min_row);
        desktopWidth = Math.round(column * uSize);
        desktopHeight = Math.round(row * uSize);
        desktopX = Math.round(taskbarWinBounds.x - desktopWidth - uGap);
        desktopY = Math.round(taskbarWinBounds.y + taskbarWinBounds.height - desktopHeight);
      }
      const desktopBounds = {
        width: desktopWidth,
        height: desktopHeight,
        x: desktopX,
        y: desktopY,
      };

      desktopWin.show();
      desktopWin.focus();
      desktopWin.moveToTop("click");
      desktopWin.setBounds(desktopBounds, true);
    }
  }

  private _createDesktopView = once(async (fromBounds: Electron.Rectangle) => {
    const desktopProdUrl = this.desktopServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href;
    const desktopUrl = await tryDevUrl(desktopProdUrl, `http://localhost:3600/index.html`);

    const desktopWin = await createNativeWindow(this.mm.mmid, {
      ...window_options,
      alwaysOnTop: false,
      show: false,
      /// 宽高
      ...fromBounds,
    });

    desktopWin.loadURL(
      buildUrl(desktopUrl, {
        search: {
          "api-base": desktopProdUrl,
          mmid: "desktop.browser.dweb",
        },
      }).href
    );

    /**
     * 是否置顶显示
     *
     * 注意，这跟 isFocused 不一样，它可能处于blur状态同时在top，只要不跟其它topView有交集
     */
    let onTop = false;
    desktopWin.on("blur", () => {
      onTop = false;
    });
    desktopWin.on("hide", () => {
      onTop = false;
    });
    this.win.on("focus", () => {
      if (desktopWin.isVisible()) {
        moveToTop("taskbar-focus");
      }
    });
    const moveToTop = (reason: string) => {
      desktopWin.moveTop();
      onTop = true;
    };
    desktopWin.isKiosk;

    return Object.assign(desktopWin, {
      moveToTop,
      isOnTop() {
        return onTop;
      },
    });
  });
}

const tryDevUrl = async (originUrl: string, devUrl: string) => {
  if (isElectronDev) {
    try {
      const res = await fetch(devUrl);
      if (res.status == 200) {
        originUrl = devUrl;
      }
    } catch {}
  }
  return originUrl;
};
