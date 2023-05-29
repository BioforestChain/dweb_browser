import chalk from "chalk";
import type { OutgoingMessage } from "node:http";
import type { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { IpcResponse } from "../../core/ipc/IpcResponse.ts";
import { Ipc, IpcRequest } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { log } from "../../helper/devtools.ts";
import { $Schema1ToType } from "../../helper/types.ts";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.ts";
import {
  barGetState,
  barSetState,
  biometricsMock,
  closeFocusedWindow,
  haptics,
  openDownloadPage,
  safeAreaGetState,
  safeAreaSetState,
  shareShare,
  toastShow,
  toggleTorch,
  torchState,
  virtualKeyboardGetState,
  virtualKeyboardSetState,
} from "./multi-webview.mobile.handler.ts";
import {
  deleteWapis,
  forceGetWapis,
  getAllWapis,
} from "./mutil-webview.mobile.wapi.ts";
type $APIS = typeof import("./assets/multi-webview.html.ts")["APIS"];

/**
 * 构建一个视图树
 * 如果是桌面版，所以不用去管树的概念，直接生成生成就行了
 * 但这里是模拟手机版，所以还是构建一个层级视图
 */
export class MultiWebviewNMM extends NativeMicroModule {
  mmid = "mwebview.sys.dweb" as const;
  observeMap: $ObserveMapNww = new Map();
  encoder = new TextEncoder();

  async _bootstrap(context: $BootstrapContext) {
    log.green(`${this.mmid} _bootstrap`);

    const httpDwebServer = await createHttpDwebServer(this, {});
    this._after_shutdown_signal.listen(() => httpDwebServer.close());
    /// 从本地文件夹中读取数据返回，
    /// 如果是Android，则使用 AssetManager API 读取文件数据，并且需要手动绑定 mime 与 statusCode
    (await httpDwebServer.listen()).onRequest(async (request, ipc) => {
      ipc.postMessage(
        await IpcResponse.fromResponse(
          request.req_id,
          await this.nativeFetch(
            "file:///sys/multi-webview" + request.parsed_url.pathname
          ),
          ipc
        )
      );
    });

    const root_url = httpDwebServer.startResult.urlInfo.buildInternalUrl(
      (url) => {
        url.pathname = "/index.html";
      }
    ).href;

    this.registerCommonIpcOnMessageHandler({
      pathname: "/open",
      matchMode: "full",
      input: { url: "string" },
      output: "number",
      handler: (...args) => this._open(root_url, ...args),
    });

    // 在当前焦点的 BrowserWindow对向上打开新的webveiw
    this.registerCommonIpcOnMessageHandler({
      method: "POST",
      pathname: "/open_new_webveiw_at_focused",
      matchMode: "full",
      input: { mmid: "mmid", url: "string" },
      output: "object",
      handler: openDownloadPage.bind(this, root_url),
    });

    // 关闭 ？？ 这个是关闭整个window  还是关闭一个 webview 标签
    // 用来关闭webview标签
    this.registerCommonIpcOnMessageHandler({
      pathname: "/close",
      matchMode: "full",
      input: { webview_id: "number" },
      output: "boolean",
      handler: async (args, client_ipc) => {
        const wapis = await forceGetWapis(client_ipc, root_url);
        return wapis.apis.closeWebview(args.webview_id);
      },
    });

    /**
     * 关闭当前激活的window
     */
    this.registerCommonIpcOnMessageHandler({
      pathname: "/close/focused_window",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "boolean",
      handler: closeFocusedWindow.bind(this, root_url),
    });

    // 销毁指定的 webview
    this.registerCommonIpcOnMessageHandler({
      pathname: "/destroy_webview_by_host",
      matchMode: "full",
      input: { host: "string" },
      output: "boolean",
      handler: async (args) => {
        for (const [_, wapis] of getAllWapis()) {
          await wapis.apis.destroyWebviewByHost(args.host);
        }
        return true;
      },
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/restart_webview_by_host",
      matchMode: "full",
      input: { host: "string" },
      output: "boolean",
      handler: async (args) => {
        for (const [_, wapis] of getAllWapis()) {
          await wapis.apis.restartWebviewByHost(args.host);
        }
        return true;
      },
    });

    // 通过 host 执行 javascript
    this.registerCommonIpcOnMessageHandler({
      pathname: "/webview_execute_javascript_by_webview_url",
      method: "POST",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: async (args, client_ipc, request) => {
        const host = request.headers.get("webview_url");
        if (host === null) {
          throw new Error(
            chalk.red(`
            ${
              this.mmid
            } registerCommonIpcOnMessageHandler /webview_execute_javascript_by_webview_url host === null
            args: ${JSON.stringify(args)}
            request: ${JSON.stringify(request)}
          `)
          );
        }
        const code = await request.body.text();
        // 问题新的 webveiw 没有被添加进来？？？
        // console.log('-------------', getAllWapis())
        for (const [_, wapi] of getAllWapis()) {
          wapi.apis.executeJavascriptByHost(host, code);
        }
        return true;
      },
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/status-bar.nativeui.sys.dweb/getState",
      method: "GET",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "object",
      handler: barGetState.bind(this, "statusBarGetState", root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/status-bar.nativeui.sys.dweb/setState",
      method: "GET",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "object",
      handler: barSetState.bind(this, "statusBarSetState", root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/navigation-bar.nativeui.sys.dweb/getState",
      method: "GET",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "object",
      handler: barGetState.bind(this, "navigationBarGetState", root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/navigation-bar.nativeui.sys.dweb/setState",
      method: "GET",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "object",
      handler: barSetState.bind(this, "navigationBarSetState", root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/safe-area.nativeui.sys.dweb/getState",
      method: "GET",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "object",
      handler: safeAreaGetState.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/safe-area.nativeui.sys.dweb/setState",
      method: "GET",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "object",
      handler: safeAreaSetState.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/virtual-keyboard.nativeui.sys.dweb/getState",
      method: "GET",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "object",
      handler: virtualKeyboardGetState.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/virtual-keyboard.nativeui.sys.dweb/setState",
      method: "GET",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "object",
      handler: virtualKeyboardSetState.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/toast.sys.dweb/show",
      method: "GET",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "object",
      handler: toastShow.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/share.sys.dweb/share",
      method: "POST",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "object",
      handler: shareShare.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/torch.nativeui.sys.dweb/toggleTorch",
      method: "GET",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "boolean",
      handler: toggleTorch.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/torch.nativeui.sys.dweb/torchState",
      method: "GET",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "boolean",
      handler: torchState.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/haptics.sys.dweb",
      method: "GET",
      matchMode: "prefix",
      input: { mmid: "mmid", action: "string" },
      output: "boolean",
      handler: haptics.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plubin/biommetrices",
      method: "GET",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "boolean",
      handler: biometricsMock.bind(this, root_url),
    });
  }
  /**
   * 打开 应用
   * 如果 是由 jsProcdss 调用 会在当前的 browserWindow 打开一个新的 webview
   * 如果 是由 NMM 调用的 会打开一个新的 borserWindow 同时打开一个新的 webview
   */
  private async _open(
    root_url: string,
    args: $Schema1ToType<{ url: "string" }>,
    clientIpc: Ipc,
    _request: IpcRequest
  ) {
    const wapis = await forceGetWapis(clientIpc, root_url);
    const webview_id = await wapis.apis.openWebview(args.url);
    return webview_id;
  }

  _shutdown() {
    deleteWapis(() => true);
  }
}

export interface $ObserveItem {
  res: OutgoingMessage | undefined;
  isObserve: boolean;
}

type $ObserveMapNwwItem = Map<string /** mmid */, $ObserveItem>;

type $ObserveMapNww = Map<
  // nww
  Electron.CrossProcessExports.BrowserWindow,
  $ObserveMapNwwItem
>;
