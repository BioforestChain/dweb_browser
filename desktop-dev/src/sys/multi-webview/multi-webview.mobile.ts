import chalk from "chalk";
import type { OutgoingMessage } from "node:http";
import type { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { IpcResponse } from "../../core/ipc/IpcResponse.ts";
import { Ipc, IpcEvent, IpcRequest } from "../../core/ipc/index.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { $Schema1ToType } from "../../helper/types.ts";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.ts";
import {
closeAppByIpc,
  closeFocusedWindow,
  openDownloadPage,
} from "./multi-webview.mobile.handler.ts";
import { EVENT, WebViewState } from "../../user/tool/tool.event.ts"
import {
  deleteWapis,
  forceGetWapis,
  getAllWapis,
} from "./mutil-webview.mobile.wapi.ts";
import Electron from "electron"
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
    console.always(`${this.mmid} _bootstrap`);
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
      input: { url: "string" },
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

    /** 关闭window */
    this.registerCommonIpcOnMessageHandler({
      pathname: "/close/app",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: closeAppByIpc.bind(this)
    })

    /**
     * 关闭当前激活的window
     */
    this.registerCommonIpcOnMessageHandler({
      pathname: "/close/focused_window",
      matchMode: "full",
      input: { mmid: "mmid" },
      output: "boolean",
      handler: closeFocusedWindow.bind(this),
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
        for (const [_, wapi] of getAllWapis()) {
          wapi.apis.executeJavascriptByHost(host, code);
        }
        return true;
      },
    });

    
    
   
  }
  /**
   * 打开 应用
   * 如果 是由 jsProcess 调用 会在当前的 browserWindow 打开一个新的 webview
   * 如果 是由 NMM 调用的 会打开一个新的 borserWindow 同时打开一个新的 webview
   */
  private async _open(
    root_url: string,
    args: $Schema1ToType<{ url: "string" }>,
    clientIpc: Ipc,
    _request: IpcRequest
  ) {
    // 同步是否可以不需要要了？？
    // 需要修改 通过 webview 需要 区分 ipc or window 来
    // Electron.ipcMain.on("sync:webveiw_state", (event: Electron.IpcMainEvent, webviewSate: WebViewState) => {
    //   clientIpc.postMessage(
    //     IpcEvent.fromText(
    //       EVENT.State,
    //       JSON.stringify(webviewSate)
    //     )
    //   )
    // })
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