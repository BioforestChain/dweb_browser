import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { once } from "../../helper/$once.ts";
import { createComlinkNativeWindow, createNativeWindow } from "../../helper/openNativeWindow.ts";
import { match } from "../../helper/patternHelper.ts";
import { buildUrl } from "../../helper/urlHelper.ts";
import { zq } from "../../helper/zodHelper.ts";
import { HttpDwebServer, createHttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import { getAppsInfo, openApp } from "../browser/browser.server.api.ts";
import { window_options } from "./const.ts";

export class DesktopNMM extends NativeMicroModule {
  mmid = "desktop.browser.dweb" as const;

  protected async _bootstrap(context: $BootstrapContext) {
    this._serveApi();
    const taskbarServer = await this._createTaskbarWebServer();
    const desktopServer = await this._createDesktopWebServer();

    await Electron.app.whenReady();

    const taskbarWin = await this._createTaskbarView(taskbarServer, desktopServer);
  }

  private _serveApi() {
    const query_app_id = zq.object({
      app_id: zq.mmid(),
    });
    this.onFetch((event) => {
      // match(event).with({ pathname: "/task-bar-apps" }, () => {
      //   return Response.json([{ icon: "" }]);
      // }).with("/open");
      return match(event)
        .with({ pathname: "/appsInfo" }, async () => {
          return Response.json(await getAppsInfo());
        })
        .with({ pathname: "/openApp" }, async (event) => {
          const { app_id } = parseQuery(event.searchParams, query_app_id);
          await openApp.call(this, app_id);
          return Response.json(true);
        })
        .run();
    }).internalServerError();
  }

  private async _createTaskbarWebServer() {
    const taskbarServer = await createHttpDwebServer(this, {
      subdomain: "taskbar",
      port: 433,
    });
    {
      (await taskbarServer.listen()).onFetch((event) => {
        const { pathname, search } = event.url;
        const url = `file:///sys/browser/desktop/taskbar${pathname}?mode=stream`;
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
      // taskbarServer.startResult.urlInfo.buildInternalUrl((url) => {
      //   url.pathname = "/index.html";
      // }).href,
      `http://localhost:3700/taskbar/index.html`,
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
    if (desktopWin.isVisible()) {
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
      desktopWin.setBounds(desktopBounds, true);
    }
  }

  private _createDesktopView = once(async (fromBounds: Electron.Rectangle) => {
    const desktopUrl = this.desktopServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href;
    const desktopWin = await createNativeWindow(this.mm.mmid, {
      ...window_options,
      show: false,
      /// 宽高
      ...fromBounds,
    });
    desktopWin.loadURL(
      buildUrl(`http://localhost:3600/index.html` || desktopUrl, {
        search: {
          "api-base": desktopUrl,
          mmid: "desktop.browser.dweb",
        },
      }).href
    );
    // desktopWin.focus();
    // desktopWin.on("blur", () => {
    //   desktopWin.hide();
    // });
    return desktopWin;
  });
}
