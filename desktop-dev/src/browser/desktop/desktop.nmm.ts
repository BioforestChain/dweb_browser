import os from "node:os";
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
    const isWin = os.platform() === "win32";

    const taskbarWin = await createComlinkNativeWindow(
      // taskbarServer.startResult.urlInfo.buildInternalUrl((url) => {
      //   url.pathname = "/index.html";
      // }).href,
      `http://localhost:3700/taskbar/index.html`,
      {
        autoHideMenuBar: true,
        type: "toolbar", //创建的窗口类型为工具栏窗口
        transparent: true, //设置透明
        alwaysOnTop: true, //窗口是否总是显示在其他窗口之前
        // resizable: false, // 禁止窗口大小缩放
        roundedCorners: true,
        ...(isWin
          ? {
              frame: false,
              thickFrame: false,
              backgroundMaterial: "mica", // window11 高斯模糊
            }
          : {
              frame: false, // 要创建无边框窗口
              vibrancy: "popover", // macos 高斯模糊
              visualEffectState: "active", // macos 失去焦点后仍然高斯模糊
            }),
      },
      async (win) => {
        return new TaskbarMainApis(win);
      }
    );
    // if (isWin) {
    //   user32.SetWindowCompositionAttribute(taskbarWin.getNativeWindowHandle().readInt32LE(), windowcompositon.ref());
    // }

    taskbarWin.webContents.openDevTools({ mode: "undocked" });
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
}
