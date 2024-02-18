import { ReadableStreamIpc } from "dweb/core/ipc-web/index.ts";
import { IPC_ROLE, IpcResponse } from "dweb/core/ipc/index.ts";
import { PromiseOut } from "../../helper/PromiseOut.ts";
import { bindThis } from "../../helper/bindThis.ts";
import { cacheGetter } from "../../helper/cacheGetter.ts";
import { $Callback, Signal } from "../../helper/createSignal.ts";
import { ReadableStreamOut, binaryStreamRead } from "../../helper/readableStreamHelper.ts";
import { $Coder, StateObserver } from "../../util/StateObserver.ts";
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
    return this.fetchApi("/setStyle", { search: { ...options, ...(await this.windowInfo) } }).void();
  }

  /**
   * 窗口聚焦
   */
  @bindThis
  async focusWindow() {
    return this.fetchApi("/focus", { search: await this.windowInfo }).void();
  }
  /**窗口失焦 */
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
  /** */
  private async wsToIpc(url: string) {
    const afterOpen = new PromiseOut<void>();
    const ipc = new ReadableStreamIpc(
      {
        mmid: "localhost.dweb",
        ipc_support_protocols: { cbor: false, protobuf: false, raw: false },
        dweb_deeplinks: [],
        categories: [],
        name: "",
      },
      //@ts-ignore
      IPC_ROLE.CLIENT
    );
    const ws = new WebSocket(url);
    ws.binaryType = "arraybuffer";
    const streamout = new ReadableStreamOut();
    ws.onmessage = (event) => {
      const data = event.data;
      streamout.controller.enqueue(data);
    };
    ws.onclose = async () => {
      streamout.controller?.close();
    };
    ws.onerror = async (event) => {
      streamout.controller.error(event);
    };
    ws.onopen = async () => {
      afterOpen.resolve();
      for await (const data of binaryStreamRead(ipc.stream)) {
        ws.send(data);
      }
    };
    ipc.bindIncomeStream(streamout.stream);
    await afterOpen.promise;
    return ipc;
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
    registryUrl.protocol = "ws";
    registryUrl.searchParams.set("id", callbackId);
    console.log("window#createModalArgs=>", registryUrl.href);
    const callbackIpc = await this.wsToIpc(registryUrl.href);
    const onCallback = new Signal<$Callback<[$ModalCallback]>>();
    callbackIpc.onRequest(async (request, ipc) => {
      const callbackData = JSON.parse(await request.body.text());
      onCallback.emit(callbackData);
      callbackIpc.postMessage(IpcResponse.fromText(request.req_id, 200, undefined, "", ipc));
    });
    const modal = await this.fetchApi(`/createModal`, {
      search: { type, ...input, callbackUrl, open, once },
    }).object<O>();

    return {
      onCallback,
      modal,
    };
  }

  /**关闭窗口 */
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
  /**打开窗口 */
  @bindThis
  async createBottomSheets(contentUrl: string) {
    const args = await this.createModalArgs<$BottomSheetsOptions, $BottomSheetsModal>("bottom-sheets", {}, true, true);
    const bottomSheets = new WindowBottomSheetsController(this, args.modal, args.onCallback);
    // 提供渲染内容
    await this.fetchApi("/open", {
      pathPrefix: "webview.sys.dweb",
      search: { url: contentUrl, rid: bottomSheets.modal.renderId },
    }).void();
    return bottomSheets;
  }
  /**
   * 在browser 打开新页面
   * @param url http?://xxxx
   */
  openInBrowser(url: string) {
    this.fetchApi("/openinbrowser", {
      pathPrefix: "web.browser.dweb",
      search: { url: url },
    });
  }
}

export const windowPlugin = new WindowPlugin();
