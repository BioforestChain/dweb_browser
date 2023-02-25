// 状态栏 主程序
// import { resolveToRootFile } from "../../helper/createResolveTo.cjs";
// import { JsMicroModule } from "../../sys/micro-module.js.cjs";

// export const statusbarJMM = new JsMicroModule("statusbar.sys.dweb", {
//   main_url: resolveToRootFile("bundle/statusbar.worker.js").href,
// } as const);
import fsPromises from "node:fs/promises"
import path from "node:path"
import process from "node:process";
import { pathToFileURL } from "node:url";
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { createResolveTo } from "../../helper/createResolveTo.cjs";
import { locks } from "../../helper/locksManager.cjs";
import {
  $NativeWindow,
  openNativeWindow,
} from "../../helper/openNativeWindow.cjs";
import { createHttpDwebServer } from "../http-server/$listenHelper.cjs";
const resolveTo = createResolveTo(__dirname);


import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { Remote } from "comlink";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs"
 
 


// @ts-ignore
type $APIS = typeof import("./assets/multi-webview.html.mjs")["APIS"];
/**
 * 构建一个视图树
 * 如果是桌面版，所以不用去管树的概念，直接生成生成就行了
 * 但这里是模拟手机版，所以还是构建一个层级视图
 */
export class StatusbarNMM extends NativeMicroModule {
  mmid = "statusbar.sys.dweb" as const;
  private _uid_wapis_map = new Map<
    number,
    { nww: $NativeWindow; apis: Remote<$APIS> }
  >();

  private _close_dweb_server?: () => unknown;

  private _lastIpc: Ipc | undefined
  private _lastRequest: IpcRequest | undefined
  private _operationQueue: ReadableStream[] = []
  // 必须要通过这样启动才可以
  // jsProcess.fetch(`file://statusbar.sys.dweb/}`) 主要必须要有最后面的路径
  async _bootstrap() {
    const { origin, listen, close } = await createHttpDwebServer(this, {});
    this._close_dweb_server = close;
    /// 从本地文件夹中读取数据返回，
    /// 如果是Android，则使用 AssetManager API 读取文件数据，并且需要手动绑定 mime 与 statusCode
    
    ;(await listen()).onRequest(async (request, ipc) => {

      // 监听 http:// 协议的请求
      // 通过 fetch("http://statusbar.sys.dweb-80.localhost:22605/") 访问的请求会被发送到这个位置
      // console.log('[statusbar.main.cts onRequest---]: ', request)
      if(request.parsed_url.pathname === "/" || request.parsed_url.pathname === "/index.html"){
        ipc.postMessage(
          await IpcResponse.fromText(
            request.req_id,
            200,
            await reqadHtmlFile(),
            new IpcHeaders({
              "Content-type": "text/html"
            })
          )
        
        );
        return ;
      }

      // 最好有一个时间限定防止超时过期
      if(request.parsed_url.pathname === "/operation"){
        console.log('[statusbar.main.cts]接受到了 /operation http 请求')

        if(this._operationQueue.length !== 0){
          ipc.postMessage(
            await IpcResponse.fromStream(
              request.req_id,
              200,
              request.body as ReadableStream<Uint8Array>,
              new IpcHeaders({
                "Content-type": "application/json"
              }),
              ipc
            )
          )
          return ;
        }

        this._lastIpc = ipc
        this._lastRequest = request;
      } 
     
    });

    // const root_url = new URL("/index.html", origin).href;
    // 下面注册的是 
    // jsProcess.fetch(`file://statusbar.sys.dweb/open?***}`) 事件监听器 
    // 监听启动请求 - 必须要有一个注册否则调用的地方 wati 就死了;
    // 监听请求页面
    this.registerCommonIpcOnMessageHandler({
      pathname: "/",
      matchMode: "full",
      input: { url: "string" },
      output: "number",
      handler: async (args, client_ipc, request) => {
        return  IpcResponse.fromText(
          request.req_id,
          200,
          await reqadHtmlFile(),
          new IpcHeaders({
            "Content-type": "text/html"
          })
        )
      },
    });

    // 监听设置状态栏
    this.registerCommonIpcOnMessageHandler({
      pathname: "/operation",
      method: "PUT",
      matchMode: "full", // 是需要匹配整个pathname 还是 前缀匹配即可
      input: {},
      output: "boolean",
      handler: async (args, client_ipc, request) => {
        console.log('[statusbar.main.cts 接受到了 /operation 操作]', args, request)

        
        // console.log('[statusbar.main.cts 接受到了 /operation 操作]', await readStream(request.body as ReadableStream<Uint8Array>))
        // 如果
        console.log(this._lastIpc, this._lastRequest)
        if(this._lastIpc === undefined || this._lastRequest === undefined){
          // 把请求保存到队列中
          this._operationQueue.push(request.body as ReadableStream<Uint8Array>)
          return true;
        }

        this._lastIpc.postMessage(
          await IpcResponse.fromStream(
            this._lastRequest.req_id,
            200,
            request.body as ReadableStream<Uint8Array>,
            new IpcHeaders({
              "Content-type": "application/json"
            }),
            this._lastIpc
          )
        )
           
        this._lastIpc = undefined 
        this._lastRequest = undefined

        // 直接调用 
        return true;
      }
    })

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
    return locks.request("multi-webview-get-window-" + ipc.uid, async () => {
      let wapi = this._uid_wapis_map.get(ipc.uid);
      if (wapi === undefined) {
        const nww = await openNativeWindow(root_url, {
          // id: "multi-webview",
          // show_in_taskbar: true,
          // new_instance: true,
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


// 读取 html 文件
async function reqadHtmlFile(){
  const targetPath = path.resolve(process.cwd(), "./src/sys/statusbar/assets/index.html")
  const content = await fsPromises.readFile(targetPath)
  return new TextDecoder().decode(content)
}

/**
 * 读取 ReadableStream
 */
async function readStream(stream:ReadableStream){
  let data: Uint8Array = new Uint8Array() 
  const reader = stream.getReader()
  let loop: boolean;
  do{
    const {value, done} = await reader.read()
    value ? data = Uint8Array.from([...data, ...value]) : null;
    loop = !done
  }while(loop)
  reader.releaseLock()
  return new TextDecoder().decode(data)
}


export interface $Operation{
  acction: string;
  value: string;
}
