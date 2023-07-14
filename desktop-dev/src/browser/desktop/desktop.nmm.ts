import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { createComlinkNativeWindow } from "../../helper/openNativeWindow.ts";
import { createHttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";

export class DesktopNMM extends NativeMicroModule {
  mmid = "desktop.browser.dweb" as const;

  protected async _bootstrap(context: $BootstrapContext) {
    const taskbarServer = await createHttpDwebServer(this, {
      subdomain: "www",
      port: 433,
    });
    {
      (await taskbarServer.listen()).onFetch((event) => {
        const { pathname, search } = event.url;
        const url = `file:///sys/browser/desktop/taskbar${pathname}?mode=stream`;
        return this.nativeFetch(url);
      });
    }
    this.onFetch(async (event) => {
      if (event.pathname === "/task-bar-apps") {
        return Response.json([{ icon: "" }]);
      }
    });

    await Electron.app.whenReady();
    const taskbar = await createComlinkNativeWindow(
      // taskbarServer.startResult.urlInfo.buildInternalUrl((url) => {
      //   url.pathname = "/index.html";
      // }).href,
      `http://localhost:5173/taskbar/index.html`,
      {
        width: 120,
        height: 120,
        type: "toolbar", //创建的窗口类型为工具栏窗口
        frame: false, //要创建无边框窗口
        resizable: false, //禁止窗口大小缩放
        transparent: true, //设置透明
        alwaysOnTop: true, //窗口是否总是显示在其他窗口之前
        vibrancy: "popover", // macos 高斯模糊
        backgroundMaterial: "mica", // window11 高斯模糊
        visualEffectState: "active",
      },
      async (win) => {
        return new TaskbarMainApis(win);
      }
    );
    // taskbar.webContents.openDevTools({ mode: "detach" });
  }

  protected _shutdown() {}
}

export class TaskbarMainApis {
  constructor(private win: Electron.BrowserWindow) {}
  resize(width: number, height: number) {
    this.win.setBounds({
      ...this.win.getBounds(),
      width,
      height,
    });
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
}
