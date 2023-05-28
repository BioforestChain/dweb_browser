import chalk from "chalk";
import type { Remote } from "comlink";
import { pathToFileURL } from "https://deno.land/std@0.177.0/node/url.ts";
import type { IncomingMessage, OutgoingMessage } from "node:http";
import path from "node:path";
import type { $BootstrapContext } from "../../core/bootstrapContext.ts";
import type { Ipc } from "../../core/ipc/ipc.ts";
import { IpcResponse } from "../../core/ipc/IpcResponse.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { log } from "../../helper/devtools.ts";
import { locks } from "../../helper/locksManager.ts";
import {
  $NativeWindow,
  openNativeWindow,
} from "../../helper/openNativeWindow.ts";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.ts";
import {
  barGetState,
  barSetState,
  biometricsMock,
  closeFocusedWindow,
  haptics,
  open,
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
  _uid_wapis_map = new Map<
    number,
    { nww: $NativeWindow; apis: Remote<$APIS> }
  >();

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
      handler: open.bind(this, root_url),
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
        const wapis = await this.forceGetWapis(client_ipc, root_url);
        return wapis.apis.closeWebview(args.webview_id);
      },
    });

    /**
     * 关闭当前激活的window
     */
    this.registerCommonIpcOnMessageHandler({
      pathname: "/close/focused_window",
      matchMode: "full",
      input: {},
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
        for (const wapis of this._uid_wapis_map.values()) {
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
        for (const wapis of this._uid_wapis_map.values()) {
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
        // console.log('-------------', this._uid_wapis_map.values())
        Array.from(this._uid_wapis_map.values()).forEach(({ apis }) => {
          apis.executeJavascriptByHost(host, code);
        });
        return true;
      },
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/status-bar.nativeui.sys.dweb/getState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: barGetState.bind(this, "statusBarGetState", root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/status-bar.nativeui.sys.dweb/setState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: barSetState.bind(this, "statusBarSetState", root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/navigation-bar.nativeui.sys.dweb/getState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: barGetState.bind(this, "navigationBarGetState", root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/navigation-bar.nativeui.sys.dweb/setState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: barSetState.bind(this, "navigationBarSetState", root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/safe-area.nativeui.sys.dweb/getState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: safeAreaGetState.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/safe-area.nativeui.sys.dweb/setState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: safeAreaSetState.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/virtual-keyboard.nativeui.sys.dweb/getState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: virtualKeyboardGetState.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/virtual-keyboard.nativeui.sys.dweb/setState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: virtualKeyboardSetState.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/toast.sys.dweb/show",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "object",
      handler: toastShow.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/share.sys.dweb/share",
      method: "POST",
      matchMode: "full",
      input: {},
      output: "object",
      handler: shareShare.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/torch.nativeui.sys.dweb/toggleTorch",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: toggleTorch.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/torch.nativeui.sys.dweb/torchState",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: torchState.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plugin/haptics.sys.dweb",
      method: "GET",
      matchMode: "prefix",
      input: { action: "string" },
      output: "boolean",
      handler: haptics.bind(this, root_url),
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/plubin/biommetrices",
      method: "GET",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: biometricsMock.bind(this, root_url),
    });
  }


  /**
   * 获取当前激活的 browserWindow 的 apis
   */
  apisGetFromFocused() {
    return Array.from(this._uid_wapis_map.values()).find((item) =>
      item.nww.isFocused()
    )?.apis;
  }

  browserWindowGetFocused() {
    console.log("this._uid_wapis_map", this._uid_wapis_map);
    return Array.from(this._uid_wapis_map.values()).find((item) =>
      item.nww.isFocused()
    )?.nww;
  }

  _shutdown() {
    this._uid_wapis_map.forEach((wapi) => {
      wapi.nww.close();
    });
    this._uid_wapis_map.clear();
  }

  // 是不是可以获取 multi-webviw.html 中的全部api
  // this._uid_wapis_map 是更具 ipc.uid 作为键明保存的，
  // 但是在一个 BrowserWindow 的内部会有多个 ipc
  // 这样会导致问题出现 会多次触发 openNativeWindow
  forceGetWapis(ipc: Ipc, root_url: string) {
    return locks.request("multi-webview-get-window-" + ipc.uid, async () => {
      let wapi = this._uid_wapis_map.get(ipc.uid);
      if (wapi === undefined) {
        const diaplay = Electron.screen.getPrimaryDisplay()
        
        const nww = await openNativeWindow(root_url, {
          webPreferences: {
            webviewTag: true,
          },
          autoHideMenuBar: true,
          // 测试代码
          width: 375,
          height: 800,
          x: 0,
          y: (diaplay.size.height - 800) / 2,
          frame: false,
        });

        const apis = nww.getApis<$APIS>();
        const absolutePath = pathToFileURL(
          path.resolve(__dirname, "./assets/preload.js")
        ).href;
        /// TIP: 这里通过类型强行引用 preload，目的是确保依赖关系，使得最终能产生编译内容
        type _Preload = typeof import("./assets/preload.ts");
        apis.preloadAbsolutePathSet(absolutePath);

        this._uid_wapis_map.set(ipc.uid, (wapi = { nww, apis }));
      }
      return wapi;
    });
  }

  getWapisByUid(uid: number) {
    console.log("this._uid_wapis_map: ", this._uid_wapis_map, "---", uid);
    return this._uid_wapis_map.get(uid);
  }
}

function getOriginByReq(req: IncomingMessage) {
  return req.headers.origin ?? new URL(req.headers.referer as string).origin;
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
