import { once } from "../../helper/$once.ts";
import { tryDevUrl } from "../../helper/electronIsDev.ts";
import { createNativeWindow } from "../../helper/openNativeWindow.ts";
import { buildUrl } from "../../helper/urlHelper.ts";
import { HttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import { window_options } from "./const.ts";
import { DeskNMM } from "./desk.nmm.ts";

export class TaskbarApi {
  constructor(
    private mm: DeskNMM,
    private win: Electron.BrowserWindow,
    private taskbarServer: HttpDwebServer,
    private desktopServer: HttpDwebServer
  ) {}
  /**
   * 对Taskbar自身进行resize
   * 根据web元素的大小进行自适应调整
   */
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

  /**
   * 打开DesktopView视图
   * PS：该函数的实现是 win/mac 专属，Android/IOS 请看下面文档的介绍来实现
   *
   * @returns 返回当前所有窗口的bounds信息
   *
   * - 在桌面端，需要显示这个视图并置顶
   * - 在移动端，将其它视图临时最小化到 TaskbarView/TooggleDesktopButton 按钮里头，在此点击该按钮可以释放这些临时视图到原本的状态
   */
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
      url.pathname = "/desktop.html";
    }).href;
    const desktopUrl = await tryDevUrl(desktopProdUrl, `http://localhost:3600/desktop.html`);

    const desktopWin = await createNativeWindow(this.mm.mmid, {
      ...window_options,
      vibrancy: undefined,
      visualEffectState: undefined,
      backgroundMaterial: undefined,
      alwaysOnTop: false,
      show: false,
      /// 宽高
      ...fromBounds,
    });

    desktopWin.loadURL(
      buildUrl(desktopUrl, {
        search: {
          "api-base": desktopProdUrl,
          mmid: "desk.browser.dweb",
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
