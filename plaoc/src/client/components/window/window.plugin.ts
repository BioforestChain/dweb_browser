import { bindThis } from "../../helper/bindThis.ts";
import { cacheGetter } from "../../helper/cacheGetter.ts";
import { $Coder, StateObserver } from "../../util/StateObserver.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { Alert } from "./Alert.ts";
import { BottomSheets } from "./BottomSheets.ts";
import type {
  $AlertModal,
  $AlertOptions,
  $BottomSheetsModal,
  $DisplayState,
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

  @bindThis
  async getDisplay<S extends $DisplayState>() {
    return await this.fetchApi("/display", { search: await this.windowInfo }).object<S>();
  }

  get getState() {
    return this.state.getState;
  }

  @bindThis
  async setStyle(options: {
    topBarOverlay?: boolean;
    bottomBarOverlay?: boolean;
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
    return this.fetchApi("/setStyle", { search: { ...options, ...(await this.windowInfo) } }).void();
  }

  /**
   * 聚焦窗口
   */
  @bindThis
  async focusWindow() {
    return this.fetchApi("/focus", { search: await this.windowInfo }).void();
  }
  /**模糊窗口 */
  @bindThis
  async blurWindow() {
    return this.fetchApi("/blur", { search: await this.windowInfo }).void();
  }
  /**最大化窗口 */
  @bindThis
  async maximize() {
    return this.fetchApi("/maximize", { search: await this.windowInfo }).void();
  }
  /**取消最大化窗口 */
  @bindThis
  async unMaximize() {
    return this.fetchApi("/unMaximize", { search: await this.windowInfo }).void();
  }
  /**隐藏或显示窗口 */
  @bindThis
  async visible() {
    return this.fetchApi("/visible", { search: await this.windowInfo }).void();
  }
  /**关闭窗口 */
  @bindThis
  async close() {
    return this.fetchApi("/close", { search: await this.windowInfo }).void();
  }
  /**关闭窗口 */
  @bindThis
  async createAlert(options: $AlertOptions) {
    const confirmCallbackUrl = new URL(
      `/internal/callback?id=${encodeURIComponent(`confirm-alert-${Math.random().toString(26).substring(2)}`)}`,
      BasePlugin.url
    );
    const dismissCallbackUrl = new URL(
      `/internal/callback?id=${encodeURIComponent(`dismiss-alert-${Math.random().toString(26).substring(2)}`)}`,
      BasePlugin.url
    );
    // 注册回调地址
    const onConfirm = this.fetchApi("/registry-callback", {
      pathPrefix: "internal",
      search: { id: confirmCallbackUrl.searchParams.get("id") },
    }).void();
    const onDismiss = this.fetchApi("/registry-callback", {
      pathPrefix: "internal",
      search: { id: dismissCallbackUrl.searchParams.get("id") },
    }).void();
    const onResult = Promise.any([onConfirm.then(() => true), onDismiss.then(() => false)]);
    const modal = await this.fetchApi("/createAlert", {
      search: { ...options, open: true, confirmCallbackUrl, dismissCallbackUrl },
    }).object<$AlertModal>();
    return new Alert(modal, onResult);
  }
  /**关闭窗口 */
  @bindThis
  async createBottomSheets(contentUrl: string) {
    const dismissCallbackUrl = new URL(
      `/internal/callback?id=${encodeURIComponent(`dismiss-bottomsheets-${Math.random().toString(26).substring(2)}`)}`,
      BasePlugin.url
    );
    // 注册回调地址
    const onDismiss = this.fetchApi("/registry-callback", {
      pathPrefix: "internal",
      search: { id: dismissCallbackUrl.searchParams.get("id") },
    }).void();
    // 创建modal
    const modal = await this.fetchApi("/createBottomSheets", {
      search: { dismissCallbackUrl },
    }).object<$BottomSheetsModal>();
    // 提供渲染内容
    await this.fetchApi("/open", {
      pathPrefix: "webview.sys.dweb",
      search: { url: contentUrl, rid: modal.renderId },
    }).void();
    return new BottomSheets(modal, onDismiss);
  }
}

export const windowPlugin = new WindowPlugin();
