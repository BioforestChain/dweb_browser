import type { Remote } from "comlink";
import { pathToFileURL } from "node:url";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { createResolveTo } from "../../helper/createResolveTo.cjs";
import { locks } from "../../helper/locksManager.cjs";
import {
  $NativeWindow,
  openNativeWindow,
} from "../../helper/openNativeWindow.cjs";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.cjs";
const resolveTo = createResolveTo(__dirname);

// @ts-ignore
type $APIS = typeof import("./assets/multi-webview.html.mjs")["APIS"];
/**
 * 构建一个视图树
 * 如果是桌面版，所以不用去管树的概念，直接生成生成就行了
 * 但这里是模拟手机版，所以还是构建一个层级视图
 */
export class MultiWebviewNMM extends NativeMicroModule {
  mmid = "mwebview.sys.dweb" as const;
  private _uid_wapis_map = new Map<
    number,
    { nww: $NativeWindow; apis: Remote<$APIS> }
  >();

  async _bootstrap() {
    const httpDwebServer = await createHttpDwebServer(this, {});
    this._after_shutdown_signal.listen(() => httpDwebServer.close());
    /// 从本地文件夹中读取数据返回，
    /// 如果是Android，则使用 AssetManager API 读取文件数据，并且需要手动绑定 mime 与 statusCode
    (await httpDwebServer.listen()).onRequest(async (request, ipc) => {
      ipc.postMessage(
        await IpcResponse.fromResponse(
          request.req_id,
          await this.fetch(
            pathToFileURL(
              resolveTo(
                "../../../" +
                  "/bundle/multi-webview" +
                  request.parsed_url.pathname
              )
            )
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
      handler: async (args, client_ipc, request) => {
        const wapis = await this.forceGetWapis(client_ipc, root_url);
        return wapis.apis.openWebview(args.url);
      },
    });
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
  }
  _shutdown() {
    this._uid_wapis_map.forEach((wapi) => {
      wapi.nww.close();
    });
    this._uid_wapis_map.clear();
  }
  private forceGetWapis(ipc: Ipc, root_url: string) {
    return locks.request("multi-webview-get-window-" + ipc.uid, async () => {
      let wapi = this._uid_wapis_map.get(ipc.uid);
      if (wapi === undefined) {
        const nww = await openNativeWindow(root_url, {
          webPreferences: {
            webviewTag: true,
          },
          autoHideMenuBar: true,
        });
        nww.maximize();

        // 打开 开发工具
        nww.webContents.openDevTools()

        const apis = nww.getApis<$APIS>();
        this._uid_wapis_map.set(ipc.uid, (wapi = { nww, apis }));
      }
      return wapi;
    });
  }
}
