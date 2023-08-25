import { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { $MMID } from "../../core/types.ts";
import { debounce } from "../../helper/$debounce.ts";
import { once } from "../../helper/$once.ts";
import { tryDevUrl } from "../../helper/electronIsDev.ts";
import { createNativeWindow } from "../../helper/openNativeWindow.ts";
import { buildUrl } from "../../helper/urlHelper.ts";
import { HttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import { DesktopApi } from "./api.desktop.ts";
import { window_options } from "./const.ts";
import { DeskNMM } from "./desk.nmm.ts";
import { deskStore } from "./desk.store.ts";
import { $DeskAppMetaData } from "./types.ts";

export class TaskbarApi {
  constructor(
    private win: Electron.BrowserWindow,
    private mm: DeskNMM,
    private context: $BootstrapContext,
    private taskbarServer: HttpDwebServer,
    private desktopServer: HttpDwebServer
  ) {
    /**
     * 绑定 runningApps 集合
     */
    mm.runingApps.onChange((map) => {
      /// 将新增的打开应用追加到列表签名
      for (const mmid of map.add) {
        this._appList.unshift(mmid);
      }
      //移除关闭的应用
      this._appList = this._appList.filter((mmid) => {
        if (map.delete.includes(mmid)) {
          return false;
        }
        return true;
      });
      /// 保存到数据库
      deskStore.set("taskbar/apps", new Set(this._appList));
    });
  }
  static async create(
    mm: DeskNMM,
    context: $BootstrapContext,
    taskbarServer: HttpDwebServer,
    desktopServer: HttpDwebServer
  ) {
    const taskbarProdUrl = taskbarServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/taskbar.html";
    }).href;
    const taskbarUrl = await tryDevUrl(taskbarProdUrl, `http://localhost:3600/taskbar.html`);

    const taskbarWin = await createNativeWindow(mm.mmid + "/taskbar", {
      ...window_options,
      /// 如果小于 80，macos会失去高斯模糊的特效
      // minHeight: 80,
      width:65,
      defaultBounds: { width: 65, height: 60 },
      resizable:false,
    });
    taskbarWin.setVisibleOnAllWorkspaces(true);

    void taskbarWin.loadURL(
      buildUrl(taskbarUrl, {
        search: {
          "api-base": taskbarProdUrl,
          mmid: mm.mmid,
        },
      }).href
    );
    return new TaskbarApi(taskbarWin, mm, context, taskbarServer, desktopServer);
  }

  /** 展示在taskbar中的应用列表 */
  private _appList = [...deskStore.get("taskbar/apps", () => new Set())];

  async getTaskbarAppList(limit: number) {
    const apps = new Map<$MMID, $DeskAppMetaData>();
    for (const app_id of this._appList) {
      if (apps.size >= limit) {
        break;
      }
      if (app_id === this.mm.mmid || apps.has(app_id)) {
        continue;
      }
      const metaData = await this.context.dns.query(app_id);
      if (metaData) {
        apps.set(app_id, { ...metaData, running: this.mm.runingApps.has(app_id), winStates: [] });
      }
    }
    return [...apps.values()];
  }
  /**
   * 对Taskbar自身进行resize
   * 根据web元素的大小进行自适应调整
   *
   * @returns 如果视图发生了真实的改变（不论是否变成说要的结果），则返回 true
   */
  _resize = async (width: number, height: number) => {
    // 获取窗口边界
    const beforeBounds = this.win.getBounds();
    // 获取显示屏信息
    const display = Electron.screen.getDisplayNearestPoint(beforeBounds);
    const uGap = width / 5;
    // height = Math.min(height, (display.workArea.height * 4) / 5);
    // 获取显示屏边界
    const x = display.workArea.width - width - uGap;
    const y = (display.workArea.height - height) / 2;
    this.win.setBounds(
      {
        x: Math.round(x),
        y: Math.round(y),
        height,
        width,
      },
      true
    );

    const afterBounds = this.win.getBounds();

    return { width: afterBounds.width, height: afterBounds.height };
  };
  resize = debounce(this._resize, 200);
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
   * @returns 返回当前受影响窗口的bounds信息
   *
   * - 在桌面端，需要显示这个视图并置顶
   * - 在移动端，将其它视图临时最小化到 TaskbarView/TooggleDesktopButton 按钮里头，在此点击该按钮可以释放这些临时视图到原本的状态
   */
  async toggleDesktopView() {
    const desktopWin = await this._createDesktopView();
    /// TODO 这里isOnTop很难正确检测，因此正确的做法是把desktop-view直接集成到taskbar窗口中
    if (desktopWin.isOnTop()) {
      desktopWin.hide();
    } else {
      const taskbarWinBounds = this.win.getBounds();
      /// 获取对应的屏幕
      const display = Electron.screen.getDisplayNearestPoint(taskbarWinBounds);
      /// 我们默认将桌面显示在taskbar左上角
      let desktopWidth = 0;
      let desktopHeight = 0;
      let desktopX = 0;
      let desktopY = 0;
      // {
      //   const uGap = taskbarWinBounds.width / 3;
      //   const uSize = 1;
      //   const { width, height } = display.workAreaSize;
      //   const min_column = uGap * 12;
      //   const max_column = Math.floor((taskbarWinBounds.x - taskbarWinBounds.width * 0.5) / uSize);
      //   const min_row = Math.floor(taskbarWinBounds.height / uSize);
      //   const max_row = Math.floor((taskbarWinBounds.y + taskbarWinBounds.height) / uSize);
      //   const rec_column = Math.floor((max_column * 2) / 3);
      //   const rec_row = Math.floor((max_row * 2) / 3);

      //   const column = Math.min(Math.max(min_column, rec_column), max_column);
      //   const row = Math.max(Math.min(max_row, rec_row), min_row);
      //   desktopWidth = Math.round(column * uSize);
      //   desktopHeight = Math.round(row * uSize);
      //   desktopX = Math.round(taskbarWinBounds.x - desktopWidth - uGap);
      //   desktopY = Math.round(taskbarWinBounds.y + taskbarWinBounds.height - desktopHeight);
      // }
      {
        const uGap = taskbarWinBounds.width / 4;
        desktopWidth = display.workArea.width / 2;
        desktopHeight = Math.max(display.workArea.height / 2, taskbarWinBounds.height);
        desktopX = taskbarWinBounds.x - desktopWidth - uGap;

        //#region 寻找视觉中线 来定位 Y

        /** 对齐 taskbar 顶部 */
        const alignTopY = taskbarWinBounds.y;
        /** 对齐 taskbar 中线 */
        const alignMidY = taskbarWinBounds.y + taskbarWinBounds.height / 2 - desktopHeight / 2; // (display.workArea.height - desktopHeight) / 2;
        /** 对齐 taskbar 底部 */
        const alignBtmY = taskbarWinBounds.y + taskbarWinBounds.height - desktopHeight;

        if (alignMidY <= 0) {
          desktopY = alignTopY;
        }
        if (alignMidY + desktopHeight >= display.workArea.height) {
          desktopY = alignBtmY;
        } else {
          desktopY = alignMidY;
        }

        const desktopHalfHeight = desktopHeight / 2;
        /** 屏幕中间的地方 */
        const displayMidY = display.workArea.height / 2 - desktopHalfHeight;
        const alignTopWeight = Math.abs(displayMidY - alignTopY);
        const alignMidWeight = Math.abs(displayMidY - alignMidY);
        const alignBtmWeight = Math.abs(displayMidY - alignBtmY);
        const minminalWeight = Math.min(alignTopWeight, alignMidWeight, alignBtmWeight);
        if (minminalWeight === alignTopWeight) {
          desktopY = alignTopY;
        } else if (minminalWeight === alignBtmWeight) {
          desktopY = alignBtmY;
        } else {
          desktopY = alignMidY;
        }
        //#endregion
      }
      const desktopBounds = {
        width: Math.round(desktopWidth),
        height: Math.round(desktopHeight),
        x: Math.round(desktopX),
        y: Math.round(desktopY),
      };

      desktopWin.show();
      desktopWin.focus();
      desktopWin.moveToTop("click");
      desktopWin.setBounds(desktopBounds, true);
    }

    return [];
  }

  private _createDesktopView = once(async () => {
    const fromBounds = this.win.getBounds();
    const desktopProdUrl = this.desktopServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/desktop.html";
    }).href;
    const desktopUrl = await tryDevUrl(desktopProdUrl, `http://localhost:3600/desktop.html`);

    const desktopWin = await createNativeWindow(this.mm.mmid + "/desktop", {
      ...window_options,
      vibrancy: undefined,
      visualEffectState: undefined,
      backgroundMaterial: undefined,
      alwaysOnTop: false,
      show: false,
      // modal: true,
      parent: this.win,
      /// 宽高
      ...fromBounds,
    });

    void desktopWin.loadURL(
      buildUrl(desktopUrl, {
        search: {
          "api-base": desktopProdUrl,
          mmid: this.mm.mmid,
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
      // desktopWin.hide();
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

  private _desktopApi?: DesktopApi;
  getDesktopApi = once(async () => {
    const win = await this._createDesktopView();
    return (this._desktopApi = new DesktopApi(win, this.mm, this.context, this.taskbarServer));
  });

  close() {
    this._desktopApi?.close();
    return this.win.close();
  }
}
