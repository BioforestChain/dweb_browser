import { IpcResponse } from "@dweb-browser/core/ipc/index.ts";
import { PromiseOut } from "@dweb-browser/helper/PromiseOut.ts";
import { cacheGetter } from "@dweb-browser/helper/cacheGetter.ts";
import { Signal, type $Callback } from "@dweb-browser/helper/createSignal.ts";
import { WebSocketIpcBuilder } from "../../common/websocketIpc.ts";
import { bindThis } from "../../helper/bindThis.ts";
import { StateObserver, type $Coder } from "../../util/StateObserver.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import { WindowAlertController } from "./WindowAlertController.ts";
import { WindowBottomSheetsController } from "./WindowBottomSheetsController.ts";
import type {
  $AlertModal,
  $AlertOptions,
  $BottomSheetsModal,
  $BottomSheetsOptions,
  $DisplayState,
  $Modal,
  $ModalCallback,
  $WindowRawState,
  $WindowState,
} from "./window.type.ts";
/**
 * 访问 window 能力的插件
 */
export class WindowPlugin extends BasePlugin {
  constructor() {
    super("window.sys.dweb");
  }
  coder: $Coder<$WindowRawState, $WindowState> = {
    decode: (raw) => ({
      ...raw,
    }),
    encode: (state) => ({
      ...state,
    }),
  };
  @cacheGetter()
  get windowInfo() {
    return this.fetchApi("/window-info", { pathPrefix: "internal" }).object<{ wid: string }>();
  }
  @cacheGetter()
  get state() {
    return new StateObserver(
      this,
      () => this.fetchState<$WindowRawState>(),
      this.coder,
      async (url) => {
        url.searchParams.set("wid", (await this.windowInfo).wid);
      }
    );
  }

  @bindThis
  protected async fetchState<S extends $WindowRawState>() {
    return await this.fetchApi("/getState", { search: await this.windowInfo }).object<S>();
  }

  /**获取当前屏幕信息 */
  @bindThis
  async getDisplay<S extends $DisplayState>() {
    return await this.fetchApi("/getDisplayInfo", { search: await this.windowInfo }).object<S>();
  }

  /**
   *  设置窗口大小
   * @param resizable 窗口是否允许手动改变宽高
   * @param width 宽
   * @param height 高
   * @returns
   */
  @bindThis
  async setBounds(resizable: boolean, width?: number, height?: number) {
    return this.fetchApi("/setBounds", {
      search: {
        wid: (await this.windowInfo).wid,
        resizable,
        width,
        height,
      },
    });
  }

  get getState() {
    return this.state.getState;
  }
  /** 设置窗口样式 */
  @bindThis
  async setStyle(options: {
    topBarOverlay?: boolean;
    bottomBarOverlay?: boolean;
    keyboardOverlaysContent?: boolean;
    topBarContentColor?: string;
    topBarContentDarkColor?: string;
    topBarBackgroundColor?: string;
    topBarBackgroundDarkColor?: string;
    bottomBarContentColor?: string;
    bottomBarContentDarkColor?: string;
    bottomBarBackgroundColor?: string;
    bottomBarBackgroundDarkColor?: string;
    bottomBarTheme?: string;
    themeColor?: string;
    themeDarkColor?: string;
  }) {
    return this.fetchApi("/setStyle", { search: { ...options, ...(await this.windowInfo) } });
  }

  /**
   * 窗口聚焦
   */
  @bindThis
  async focusWindow() {
    return this.fetchApi("/focus", { search: await this.windowInfo });
  }
  /**窗口失焦 */
  @bindThis
  async blurWindow() {
    return this.fetchApi("/blur", { search: await this.windowInfo });
  }
  /**最大化窗口 */
  @bindThis
  async maximize() {
    return this.fetchApi("/maximize", { search: await this.windowInfo });
  }
  /**取消最大化窗口 */
  @bindThis
  async unMaximize() {
    return this.fetchApi("/unMaximize", { search: await this.windowInfo });
  }
  /**隐藏或显示窗口 */
  @bindThis
  async visible() {
    return this.fetchApi("/visible", { search: await this.windowInfo });
  }
  /**关闭窗口 */
  @bindThis
  async close() {
    return this.fetchApi("/close", { search: await this.windowInfo });
  }

  /**创建window的ipc连接 */
  private getWindowIpc(url: URL) {
    const wsIpcBuilder = new WebSocketIpcBuilder(url, this.self);
    return wsIpcBuilder.ipc;
  }
  private async createModalArgs<I extends $AlertOptions | $BottomSheetsOptions, O extends $Modal>(
    type: O["type"],
    input: I,
    open = false,
    once = false
  ) {
    const callbackId = `${type}-${Math.random().toString(26).substring(2)}`;
    const callbackUrl = new URL(`/internal/callback?id=${callbackId}`, BasePlugin.api_url);
    // 注册回调地址
    const registryUrl = new URL("/internal/registry-callback", BasePlugin.api_url);
    registryUrl.protocol = "wss";
    registryUrl.searchParams.set("id", callbackId);
    console.log("window#createModalArgs=>", registryUrl.href);
    const ipc = this.getWindowIpc(registryUrl);
    const onCallback = new Signal<$Callback<[$ModalCallback]>>();
    ipc.onRequest("registry-callback").collect(async (event) => {
      const request = event.consume();
      const callbackData = JSON.parse(await request.body.text());
      onCallback.emit(callbackData);
      ipc.postMessage(IpcResponse.fromText(request.reqId, 200, undefined, "", ipc));
    });
    const modal = await this.fetchApi(`/createModal`, {
      search: { type, ...input, callbackUrl, open, once },
    }).object<O>();

    return {
      onCallback,
      modal,
    };
  }

  /**弹出窗口 */
  @bindThis
  async alert(options: $AlertOptions) {
    const args = await this.createModalArgs<$AlertOptions, $AlertModal>("alert", options, true, true);
    const alert = new WindowAlertController(this, args.modal, args.onCallback);
    const result = new PromiseOut<boolean>();
    alert.addEventListener("close", () => {
      result.resolve(alert.result);
    });
    return result.promise;
  }
  /**弹出从底部出现的窗口 */
  @bindThis
  async createBottomSheets(contentUrl: string) {
    const args = await this.createModalArgs<$BottomSheetsOptions, $BottomSheetsModal>("bottom-sheets", {}, true, true);
    const bottomSheets = new WindowBottomSheetsController(this, args.modal, args.onCallback);
    // 提供渲染内容
    await this.fetchApi("/open", {
      pathPrefix: "webview.sys.dweb",
      search: { url: contentUrl, rid: bottomSheets.modal.renderId },
    });
    return bottomSheets;
  }
}

export const windowPlugin = new WindowPlugin();
