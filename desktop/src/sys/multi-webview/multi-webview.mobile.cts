import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { locks } from "../../helper/locksManager.cjs";
import {
  $NativeWindow,
  openNativeWindow,
} from "../../helper/openNativeWindow.cjs";
import { log } from "../../helper/devtools.cjs";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.cjs";
import chalk from "chalk"
import type { $BootstrapContext } from "../../core/bootstrapContext.cjs";
import type { Remote } from "comlink";
import type { Ipc } from "../../core/ipc/ipc.cjs";

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

  async _bootstrap(context: $BootstrapContext) {
    log.green(`${this.mmid} _bootstrap`)
    
    const httpDwebServer = await createHttpDwebServer(this, {});
    this._after_shutdown_signal.listen(() => httpDwebServer.close());
    /// 从本地文件夹中读取数据返回，
    /// 如果是Android，则使用 AssetManager API 读取文件数据，并且需要手动绑定 mime 与 statusCode
    (await httpDwebServer.listen()).onRequest(async (request, ipc) => {
      ipc.postMessage(
        await IpcResponse.fromResponse(
          request.req_id,
          await this.nativeFetch(
            "file:///bundle/multi-webview" + request.parsed_url.pathname
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

    // 打开一个新的window对象
    this.registerCommonIpcOnMessageHandler({
      pathname: "/open",
      matchMode: "full",
      input: { url: "string" },
      output: "number",
      handler: async (args, client_ipc, request) => {
        console.log('[multi-webview.mobile.cts 接受到了 open 请求>>>>>>>>>>>>>]--------------------------------------', args.url, client_ipc.uid)
        const wapis = await this.forceGetWapis(client_ipc, root_url);
        const webview_id = await wapis.apis.openWebview(args.url);
        console.log('multi-webview.mobile.cts /open args.url:', args.url)
        return webview_id
      },
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

    // 销毁指定的 webview
    this.registerCommonIpcOnMessageHandler({
      pathname: "/destroy_webview_by_host",
      matchMode: "full",
      input: { host: "string" },
      output: "boolean",
      handler: async (args, client_ipc) => {
        Array.from(this._uid_wapis_map.values()).forEach(wapis => {
          wapis.apis.destroyWebviewByHost(args.host)
        })

        // console.log('------ multi-webview.mobile.cts 执行了销毁', wapisArr);
        // console.log('this._uid_wapis_map: ', this._uid_wapis_map)
        // console.log('ipc.uid: ', (client_ipc as any).uid)
        // const wapis = await this.forceGetWapis(client_ipc, root_url);
        // return wapis.apis.destroyWebviewByHost(args.host);\

        return true;
      },
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/restart_webview_by_host",
      matchMode: "full",
      input: { host: "string" },
      output: "boolean",
      handler: async (args, client_ipc) => {
        Array.from(this._uid_wapis_map.values()).forEach(wapis => {
          wapis.apis.restartWebviewByHost(args.host)
        })
        return true;
      },
    })

    // 通过 host 更新
    this.registerCommonIpcOnMessageHandler({
      pathname: "/webview_execute_javascript_by_host",
      method: "POST",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: async(args, client_ipc, request) => {
        const host = request.headers.get("origin")
        if(host === null) throw new Error(`${this.mmid} registerCommonIpcOnMessageHandler /webview_execute_javascript_by_host host === null`);
        const code = await request.body.text();
        Array.from(this._uid_wapis_map.values()).forEach(({apis}) => {
          apis.executeJavascriptByHost(host, code);
        })
        return true;
      }
    })
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
        nww.webContents.openDevTools();

        const apis = nww.getApis<$APIS>();
        this._uid_wapis_map.set(ipc.uid, (wapi = { nww, apis }));
      }
      return wapi;
    });
  }
}
