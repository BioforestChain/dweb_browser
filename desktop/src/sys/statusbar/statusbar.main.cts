// 模拟状态栏模块-用来提供状态UI的模块
import fsPromises from "node:fs/promises"
import path from "node:path"
import process from "node:process";
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { locks } from "../../helper/locksManager.cjs";
import {
  $NativeWindow,
  openNativeWindow,
} from "../../helper/openNativeWindow.cjs";
import { createHttpDwebServer } from "../http-server/$listenHelper.cjs";
import chalk from "chalk"
import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { Remote } from "comlink";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs"

// @ts-ignore
type $APIS = typeof import("./assets/multi-webview.html.mjs")["APIS"];
export class StatusbarNMM extends NativeMicroModule {
  mmid = "statusbar.sys.dweb" as const;
  private _uid_wapis_map = new Map<
    number,
    { nww: $NativeWindow; apis: Remote<$APIS> }
  >();

  private _close_dweb_server?: () => unknown;
  private _statusbarHtmlRequestMap = new Map<string, $StatusbarHtmlRequest>() // statusbar.html 状态栏等待设置的返回队列
  private _statusbarPluginsRequestMap = new Map<string, $StatusbarPluginsRequestQueueItem[]>()              // statusbar.plugins发起更改状态栏设置的请求队列
  private _statusbarPluginsNoReleaseRequestMap = new Map<string, $StatusbarPluginsRequestQueueItem[]>()    // statusbar.plugins 发起更改状态栏设置已经出发更改但是还没有返回结果的队列
  private _allocId = 0;
  // 必须要通过这样启动才可以
  
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
      

      // 处理操作完成后 statusbar.html 发送过来的数据
      if(request.parsed_url.pathname === '/operation_return'){
        const id = request.headers.id
        const appUrlFromStatusbarHtml = request.parsed_url.searchParams.get("app_url")
        if(!id){
          ipc.postMessage(
            await IpcResponse.fromText(
              request.req_id,
              400,
              "headers 缺少了 id 标识符",
              new IpcHeaders({
                "Content-type": "text/plain"
              }),
            )
          )
          return;
        }

        if(appUrlFromStatusbarHtml === null){
          ipc.postMessage(
            await IpcResponse.fromText(
              request.req_id,
              400,
              "确实少 app_url 查询参数",
              new IpcHeaders({
                "Content-type": "text/plain"
              }),
            )
          )
          return;
        }

        let statusbarPluginsNoReleaseRequest = this._statusbarPluginsNoReleaseRequestMap.get(appUrlFromStatusbarHtml) as $StatusbarPluginsRequestQueueItem[]
        let itemIndex = statusbarPluginsNoReleaseRequest.findIndex(_item => _item.id === id)
        let item = statusbarPluginsNoReleaseRequest[itemIndex]
                  statusbarPluginsNoReleaseRequest.splice(itemIndex, 1)
        // 返回的就是一个 json
        const data = await readStream(request.body as ReadableStream)
        item
        .callback(
          await IpcResponse.fromJson(
            item.req_id,
            200,
            data,
            new IpcHeaders({
              "Content-type": "text/plain"
            }),
          )
        )
        // 返回 /operation_return 的请求
        ipc.postMessage(
          await IpcResponse.fromText(
            request.req_id,
            200,
            "ok",
            new IpcHeaders({
              "Content-type": "text/plain"
            }),
          )
        )
      }

      // todo 最好有一个时间限定防止超时过期
      if(request.parsed_url.pathname === "/operation_from_html"){
        const appUrlFromStatusbarHtml = request.parsed_url.searchParams.get("app_url")
        if(appUrlFromStatusbarHtml === null){
          ipc.postMessage(
            await IpcResponse.fromText(
              request.req_id,
              400,
              "确实少 app_url 查询参数",
              new IpcHeaders({
                "Content-type": "text/plain"
              }),
            )
          )
          return 
        }

        // 添加到队列中
        this._statusbarHtmlRequestMap.set(appUrlFromStatusbarHtml, {ipc: ipc, request: request, appUrl: appUrlFromStatusbarHtml})
        this._sendToStatusbarHtml(appUrlFromStatusbarHtml)
      } 
    });

    // const root_url = new URL("/index.html", origin).href;
    // 下面注册的是 
    // jsProcess.fetch(`file://statusbar.sys.dweb/open?***}`) 事件监听器 
    // 监听启动请求 - 必须要有一个注册否则调用的地方 wati 就死了;
    // 监听请求页面
    // console.log('[statusbar.main.cts registerCommonIpcOnMessageHandler path /]')
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
      pathname: "/operation_from_plugins",
      method: "PUT",
      matchMode: "full", // 是需要匹配整个pathname 还是 前缀匹配即可
      input: {},
      output: "boolean",
      handler: async (args, client_ipc, request) => {
        const appUrlFromApp = request.parsed_url.searchParams.get("app_url")
        // console.log('[statusbar.main.cts 接受到了 /operation 操作 appUrlFromApp]', appUrlFromApp)
        if(appUrlFromApp === null){ /**已经测试走过了 */
          return  IpcResponse.fromText(
            request.req_id,
            400,
            "缺少 app_url 查询参数",
            new IpcHeaders({
              "Content-type": "text/plain"
            })
          ) 
        }
        
        // 把 ststusbar.plugings 的请求保存到队列
        // 调用这个执行发送国的函数 执行发送的函数必须是await 
        // 等待这个调用的函数执行完毕后在返回？？
        let statusbarPluginRequest = this._statusbarPluginsRequestMap.get(appUrlFromApp)
        const result = await new Promise<IpcResponse>(resolve => {
          if(statusbarPluginRequest === undefined){
            statusbarPluginRequest = []
            this._statusbarPluginsRequestMap.set(appUrlFromApp, statusbarPluginRequest)
          }
          statusbarPluginRequest.push({
            body: request.body as ReadableStream<Uint8Array>,
            callback: (reponse: IpcResponse) => {
              resolve(reponse)
            },
            req_id: request.req_id,
            id: `${this._allocId++}`
          })
          
          // 执行发送的函数
          this._sendToStatusbarHtml(appUrlFromApp)
        })

        return result;
      }
    })
  }

  private async _sendToStatusbarHtml(appUrl: string){
    const statusbarHtmlRequest = this._statusbarHtmlRequestMap.get(appUrl)
    const statusbarPluginRequest = this._statusbarPluginsRequestMap.get(appUrl)
    let statusbarPluginsNoReleaseRequest = this._statusbarPluginsNoReleaseRequestMap.get(appUrl)

    if(statusbarHtmlRequest === undefined) return ;
    if(statusbarPluginRequest === undefined) return;
    const operationQueueItem = statusbarPluginRequest.shift();
    if(operationQueueItem === undefined) return;
    if(statusbarPluginsNoReleaseRequest === undefined){
      statusbarPluginsNoReleaseRequest = [];
      this._statusbarPluginsNoReleaseRequestMap.set(appUrl, statusbarPluginsNoReleaseRequest)
      
    }
    statusbarPluginsNoReleaseRequest.push(operationQueueItem)

    statusbarHtmlRequest.ipc.postMessage(
      await IpcResponse.fromStream(
        statusbarHtmlRequest.request.req_id,
        200,
        operationQueueItem.body as ReadableStream<Uint8Array>,
        new IpcHeaders({
          "Content-type": "application/json",
          "id": operationQueueItem.id,
        }),
        statusbarHtmlRequest.ipc
      )
    )
    // 需要删除map里面保存的数据 如果不删除可能导致 多次来至 statusbar.plugins 发送过来请求，会使用
    // 同一个  statusbarHtmlRequest 发生错误
    this._statusbarHtmlRequestMap.delete(appUrl);
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

export interface $StatusbarPluginsRequestQueueItem{
  body: ReadableStream<Uint8Array>
  callback: {(response: IpcResponse): void} ;
  req_id: number;
  id: string; // 队列项的标识符
}

export interface $StatusbarHtmlRequest{
  ipc:Ipc;
  request: IpcRequest;
  appUrl: string;  // appUrl 标识 当前statusbar搭配的是哪个 app 显示的
}

export enum $StatusbarStyle{
  light = "light",
  dark = "dark",
  default = 'default',
}

export type $isOverlays = "0"  // 不覆盖
                         | "1" // 覆盖

 
