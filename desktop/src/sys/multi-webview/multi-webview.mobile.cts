import type { Ipc } from "../../core/ipc/ipc.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { locks } from "../../helper/locksManager.cjs";
import { openNwWindow } from "../../helper/openNwWindow.cjs";
import { createHttpDwebServer } from "../http-server/$listenHelper.cjs";

// @ts-ignore
type $APIS = typeof import("../multi-webview.html")["APIS"];
/**
 * 构建一个视图树
 * 如果是桌面版，所以不用去管树的概念，直接生成生成就行了
 * 但这里是模拟手机版，所以还是构建一个层级视图
 */
export class MultiWebviewNMM extends NativeMicroModule {
  mmid = "mwebview.sys.dweb" as const;
  private _uid_wapis_map = new Map<number, { nww: nw.Window; apis: $APIS }>();

  private _close_dweb_server?: () => unknown;

  async _bootstrap() {
    const { origin, start, close } = await createHttpDwebServer(this, {});
    this._close_dweb_server = close;
    /// 从本地文件夹中读取数据返回，
    /// 如果是Android，则使用 AssetManager API 读取文件数据，并且需要手动绑定 mime 与 statusCode
    (await start()).onRequest(async (request, ipc) => {
      const res = await this.fetch(
        "/bundle/multi-webview" + request.parsed_url.pathname
      );
      ipc.postMessage(await IpcResponse.fromResponse(request.req_id, res, ipc));
    });

    const root_url = new URL("/index.html", origin).href;

    this.registerCommonIpcOnMessageHanlder({
      pathname: "/open",
      matchMode: "full",
      input: { url: "string" },
      output: "number",
      hanlder: async (args, client_ipc) => {
        const wapis = await this.forceGetWapis(client_ipc, root_url);
        return wapis.apis.openWebview(args.url);
      },
    });
  }
  _shutdown() {
    this._uid_wapis_map.forEach((wapi) => {
      wapi.nww.close();
    });
    this._uid_wapis_map.clear();
    this._close_dweb_server?.();
    this._close_dweb_server = undefined;
  }
  private forceGetWapis(ipc: Ipc, root_url: string) {
    return locks.request(
      "multi-webview-get-window-" + ipc.uid,
      async (lock) => {
        let wapi = this._uid_wapis_map.get(ipc.uid);
        if (wapi === undefined) {
          const nww = await openNwWindow(root_url, {
            id: "multi-webview",
            show_in_taskbar: true,
            // new_instance: true,
          });
          await new Promise((resolve) => {
            nww.on("document-end", resolve);
          });
          const apis = nww.window as $APIS;
          this._uid_wapis_map.set(ipc.uid, (wapi = { nww, apis }));
        }
        return wapi;
      }
    );
  }
}
