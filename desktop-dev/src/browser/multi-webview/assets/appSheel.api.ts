/// <reference lib="DOM"/>
const { ipcRenderer } = Electron;
import { LitElement, PropertyValues } from "lit";
import { property, state } from "lit/decorators.js";
import "../../../helper/electron.ts";
import { mainApis } from "../../../helper/openNativeWindow.preload.ts";
import type {
  $AllWebviewState,
  $BarState,
  $OverlayState,
  $ShareOptions,
  $VirtualKeyboardState,
} from "../types.ts";
import { getButtomBarState } from "./components/multi-webview-comp-safe-area.shim.ts";
import "./viewItem.html.ts";
import { ViewItem } from "./viewItem.ts";

export abstract class Api extends LitElement {
  private _id_acc = 0;
  @state() viewItems: Array<ViewItem> = [];
  //  new Proxy([], {
  //   get: (target, prop, receiver) => {
  //     if (
  //       prop === "push" ||
  //       prop === "pop" ||
  //       prop === "shift" ||
  //       prop === "unshift" ||
  //       prop === "split" ||
  //       prop === "sort" ||
  //       prop === "reverse"
  //     ) {
  //       return (...args: any[]) => {
  //         try {
  //           (target as any)[prop].apply(target, args);
  //         } finally {
  //           this.webviews_syncToMainProcess();
  //         }
  //       };
  //     }
  //     return Reflect.get(target, prop, receiver);
  //   },
  // });
  statusBarHeight = "38px";
  navigationBarHeight = "26px";
  virtualKeyboardAnimationSetIntervalId: unknown = 0;

  @property() name?: string = "Multi Webview";
  @property({ type: Object }) statusBarState: WeakMap<ViewItem, $BarState> =
    new WeakMap();
  @property({ type: Object }) navigationBarState: WeakMap<ViewItem, $BarState> =
    new WeakMap();
  @property({ type: Object }) safeAreaState: WeakMap<ViewItem, $OverlayState> =
    new WeakMap();
  @property({ type: Boolean }) isShowVirtualKeyboard = false;
  @property({ type: Object }) virtualKeyboardState: $VirtualKeyboardState = {
    insets: {
      top: 0,
      right: 0,
      bottom: 0,
      left: 0,
    },
    overlay: false,
    visible: false,
  };
  @state() torchState = { isOpen: false };
  @state() preloadAbsolutePath = "";
  nativeCloseWatcherKitData = {
    tokenToId: new Map<string, number>(),
    idToToken: new Map<number, string>(),
    allocId: 0,
  };

  /**
   * 设置state指挥设置最顶层的 state
   * @param propertyName
   * @param key
   * @param value
   * @returns
   */
  barSetState<
    $PropertyName extends keyof Pick<
      this,
      "statusBarState" | "navigationBarState"
    >,
    K extends keyof $BarState,
    V extends $BarState[K]
  >(propertyName: $PropertyName, key: K, value: V) {
    const barState = this[propertyName];
    const state = barState.get(this.viewItems[0]);
    if (state === undefined) throw new Error("state === undefined");
    state[key] = value;
    // 如果改变的 navigationBarState.visible
    // 还需要改变 insets.bottom 的值
    if (propertyName === "navigationBarState" && key === "visible") {
      state.insets.bottom = value ? parseInt(this.navigationBarHeight) : 0;
    }
    this.requestUpdate(propertyName);
    return state;
  }

  readonly statusBarSetState = this.barSetState.bind(this, "statusBarState");
  readonly statusBarGetState = this.barGetState.bind(this, "statusBarState");
  readonly navigationBarSetState = this.barSetState.bind(
    this,
    "navigationBarState"
  );
  readonly navigationBarGetState = this.barGetState.bind(
    this,
    "navigationBarState"
  );

  /**
   * 获取当前的状态
   * 就是获取 最顶层的状态
   * @param propertyName
   * @returns
   */
  barGetState<
    $PropertyName extends keyof Pick<
      this,
      "statusBarState" | "navigationBarState"
    >
  >(propertyName: $PropertyName) {
    const state = this[propertyName].get(this.viewItems[0]);
    if (state === undefined) throw new Error(`state === undefined`);
    return state;
  }

  virtualKeyboardFirstUpdated() {
    this.virtualKeyboardState = {
      ...this.virtualKeyboardState,
      visible: true,
    };
  }

  virtualKeyboardHideCompleted() {
    this.isShowVirtualKeyboard = false;
    ipcRenderer.send("safe_are_insets_change");
    clearInterval(this.virtualKeyboardAnimationSetIntervalId as number);
  }

  virtualKeyboardShowCompleted() {
    ipcRenderer.send("safe_are_insets_change");
    clearInterval(this.virtualKeyboardAnimationSetIntervalId as number);
  }

  virtualKeyboardSetOverlay(overlay: boolean) {
    const state = {
      ...this.virtualKeyboardState,
      overlay: overlay,
    };
    this.virtualKeyboardState = state;
    return state;
  }

  virtualKeyboardGetState() {
    return this.virtualKeyboardState;
  }

  safeAreaGetState = () => {
    const webview = this.viewItems[0];
    const navigationBarState = this.navigationBarState.get(webview);
    if (navigationBarState === undefined)
      throw new Error(`navigationBarState === undefined`);
    const statusbarState = this.statusBarState.get(webview);
    if (statusbarState === undefined)
      throw new Error(`statusbarState === undefined`);
    const safeareaState = this.safeAreaState.get(webview);
    if (safeareaState === undefined)
      throw new Error("safeareaState === undefined");
    const bottomBarState = getButtomBarState(
      navigationBarState,
      this.isShowVirtualKeyboard,
      this.virtualKeyboardState
    );
    return this.safeAreaGetStateByBar(
      statusbarState,
      bottomBarState,
      safeareaState
    );
    // return {
    //   overlay: this.safeAreaState[0].overlay,
    //   insets: {
    //     left: 0,
    //     top: statusbarState.visible
    //       ? statusbarState.overlay
    //         ? statusbarState.insets.top
    //         : 0
    //       : statusbarState.insets.top,
    //     right: 0,
    //     bottom: bottomBarState.visible
    //       ? bottomBarState.overlay
    //         ? bottomBarState.insets.bottom
    //         : 0
    //       : 0,
    //   },
    //   cutoutInsets: {
    //     left: 0,
    //     top: statusbarState.insets.top,
    //     right: 0,
    //     bottom: 0,
    //   },
    //   // 外部尺寸
    //   outerInsets: {
    //     left: 0,
    //     top: statusbarState.visible
    //       ? statusbarState.overlay
    //         ? 0
    //         : statusbarState.insets.top
    //       : 0,
    //     right: 0,
    //     bottom: bottomBarState.visible
    //       ? bottomBarState.overlay
    //         ? 0
    //         : bottomBarState.insets.bottom
    //       : 0,
    //   },
    // };
  };

  safeAreaGetStateByBar = (
    topBarState: Pick<$BarState, "visible" | "insets" | "overlay">,
    bottomBarState: Pick<$BarState, "visible" | "insets" | "overlay">,
    safeAreaState: $OverlayState
  ) => {
    return {
      overlay: safeAreaState.overlay,
      insets: {
        left: 0,
        top: topBarState.visible
          ? topBarState.overlay
            ? topBarState.insets.top
            : 0
          : topBarState.insets.top,
        right: 0,
        bottom: bottomBarState.visible
          ? bottomBarState.overlay
            ? bottomBarState.insets.bottom
            : 0
          : 0,
      },
      cutoutInsets: {
        left: 0,
        top: topBarState.insets.top,
        right: 0,
        bottom: 0,
      },
      // 外部尺寸
      outerInsets: {
        left: 0,
        top: topBarState.visible
          ? topBarState.overlay
            ? 0
            : topBarState.insets.top
          : 0,
        right: 0,
        bottom: bottomBarState.visible
          ? bottomBarState.overlay
            ? 0
            : bottomBarState.insets.bottom
          : 0,
      },
    };
  };

  safeAreaSetOverlay = (overlay: boolean) => {
    const state = this.safeAreaState.get(this.viewItems[0]);
    if (state === undefined) throw new Error(`state === undefined`);
    state.overlay = overlay;
    this.barSetState("statusBarState", "overlay", overlay);
    this.barSetState("navigationBarState", "overlay", overlay);
    this.virtualKeyboardSetOverlay(overlay);
    this.requestUpdate("statusBarState");
    return this.safeAreaGetState();
  };

  safeAreaNeedUpdate = () => {
    ipcRenderer.send(
      "safe_area_update",
      new URL(this.viewItems[this.viewItems.length - 1].src).host.replace(
        "www.",
        "api."
      ),
      this.safeAreaGetState()
    );
  };

  torchStateToggle() {
    const state = {
      ...this.torchState,
      isOpen: !this.torchState.isOpen,
    };
    this.torchState = state;
    return state.isOpen;
  }

  torchStateGet() {
    return this.torchState.isOpen;
  }

  barcodeScanningGetPhoto() {
    const el = document.createElement("multi-webview-comp-barcode-scanning");
    document.body.append(el);
  }

  /**
   * 删除 state 现阶段是删除第一个 还需要修改为
   */
  private state_delete() {
    const webveiw = this.viewItems[0];
    this.statusBarState.delete(webveiw);
    this.navigationBarState.delete(webveiw);
    this.safeAreaState.delete(webveiw);
  }

  /**
   * 向 webveiws 数据中插入新的数据
   * 新的数据在 webviews 数据中顶部
   * 会自动更新 UI
   * @param src
   * @returns
   */
  webveiws_unshift(src: string) {
    const webview_id = this._id_acc++;
    const viewItem = new ViewItem(webview_id, src);
    // 都从最前面插入
    this.viewItems = [viewItem, ...this.viewItems];
    /// 采用默认的值,
    /// TODO 未来这些将由plaoc控制，mwebview的状态栏色调，只由 opener 决策，如果需要多视图管理这些状态，由 opener 自己分发
    this.statusBarState.set(
      viewItem,
      createDefaultBarState("statusbar", this.statusBarHeight)
    );
    this.navigationBarState.set(
      viewItem,
      createDefaultBarState("navigationbar", this.navigationBarHeight)
    );
    this.safeAreaState.set(viewItem, createDefaultSafeAreaState());

    return webview_id;
  }

  /**
   * 根据传递的 webvdiew_id 数据
   * 从 webviews 数据中删除匹配的项
   * 关闭 匹配的 devTools window
   * 同步 最新的 webveiws 数据到主进程
   * 删除 state
   * 同步 state 到主进程
   * @param webview_id
   * @returns
   */
  webveiws_deleteById(webview_id: number) {
    const deleteIndex = this.viewItems.findIndex(
      (webview) => webview.id === webview_id
    );
    const [deleteWebview] = this.viewItems.splice(deleteIndex, 1);
    // 关闭 devTools window
    mainApis.closeDevTools(deleteWebview.webContentId);
    console.error(
      `error`,
      "this.state_delete() 必须要调整为 通过 webview_id 删除 需要把state 同webview 关联起来"
    );
    this.state_delete();
    // 把 navigationBarState statusBarStte safe-area 的改变发出
    ipcRenderer.send("safe_are_insets_change");
    ipcRenderer.send("navigation_bar_state_change");
    ipcRenderer.send("status_bar_state_change");
    return true;
  }

  webivews_deleteByHost(host: string) {
    this.viewItems.forEach((webview) => {
      const _url = new URL(webview.src);
      if (_url.host === host) {
        this.webveiws_deleteById(webview.id);
      }
    });
    return true;
  }

  webivews_deleteByOrigin(origin: string) {
    this.viewItems.forEach((webview) => {
      if (webview.src.includes(origin)) {
        this.webveiws_deleteById(webview.id);
      }
    });
    return true;
  }
  /**
   * webviws 的数据同步到 主进程
   */
  webviews_syncToMainProcess(): void {
    const uid = new URL(location.href).searchParams.get("uid");
    const allWebviewState: $AllWebviewState = {};
    this.viewItems.forEach((item, index) => {
      allWebviewState[item.id] = {
        webviewId: item.id,
        isActivated: index === 0 ? true : false,
        src: item.src,
      };
    });
    ipcRenderer.send("sync:webview_state", uid, allWebviewState);
  }

  override willUpdate(changedProperties: PropertyValues) {
    super.willUpdate(changedProperties);
    if (changedProperties.has("webviews")) {
      this.webviews_syncToMainProcess();
    }
  }

  /**
   * 关闭当前 window
   */
  window_close() {
    mainApis.closedBrowserWindow();
  }

  preloadAbsolutePathSet(path: string) {
    this.preloadAbsolutePath = path;
  }

  abstract toastShow(
    message: string,
    duration: string,
    position: "top" | "bottom"
  ): void;

  abstract shareShare(options: $ShareOptions): void;

  abstract hapticsSet(value: string): boolean;

  abstract biometricsMock(): Promise<boolean>;
}

/**
 * 创建默认的 bar 状态
 * @param barname
 * @param height
 * @returns
 */
function createDefaultBarState(
  barname: "statusbar" | "navigationbar",
  height: string
): $BarState {
  return {
    color: "#FFFFFFFF",
    style: "DEFAULT",
    insets: {
      top: barname === "statusbar" ? parseInt(height) : 0,
      right: 0,
      bottom: barname === "navigationbar" ? parseInt(height) : 0,
      left: 0,
    },
    overlay: false,
    visible: true,
  };
}

// 创建默认的 safearea 状态
function createDefaultSafeAreaState(): $OverlayState {
  return {
    overlay: false,
  };
}

export type $Api = InstanceType<typeof Api>;
