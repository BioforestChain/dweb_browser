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
  private _statusbarHtmlRequestMap = new Map<string, $StatusbarHtmlRequest>() // statusbar.html 状态栏等待设置的返回队列
  private _statusbarPluginsRequestMap = new Map<string, $StatusbarPluginsRequestQueueItem[]>()              // statusbar.plugins发起更改状态栏设置的请求队列
  private _statusbarPluginsNoReleaseRequestMap = new Map<string, $StatusbarPluginsRequestQueueItem[]>()    // statusbar.plugins 发起更改状态栏设置已经出发更改但是还没有返回结果的队列
  private _allocId = 0;
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

      // 处理操作完成后 statusbar.html 发送过来的数据
      if(request.parsed_url.pathname === '/operation_return'){
        console.log('[statusbar.main.cts /operation_return request.headers]', request.headers)
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
                  console.log('[statusbar.main.cts /operation_return appUrlFromStatusbarHtml]', appUrlFromStatusbarHtml)
                  console.log('[statusbar.main.cts /operation_return request.body]', request.body)
        console.log('[statusbar.main.cts /operation_return item]', item)
        console.log('[statusbar.main.cts /operation_return statusbarPluginsNoReleaseRequest]', statusbarPluginsNoReleaseRequest)
        // 返回的就是一个 json
        const data = await readStream(request.body as ReadableStream)
        console.log('[statusbar.main.cts /operation_return data]', data)

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
      }

      // todo 最好有一个时间限定防止超时过期
      if(request.parsed_url.pathname === "/operation"){
        console.log('[statusbar.main.cts]接受到了 /operation http 请求')
        // appUrl 标识 当前statusbar搭配的是哪个 app 显示的
        // 因为 statusbar 会提供给任意个 browserWindow 使用
        const appUrlFromStatusbarHtml = request.parsed_url.searchParams.get("app_url")
        console.log('[statusbar.main.cts]接受到了 /operation http 请求 appUrlFromStatusbarHtml === ',appUrlFromStatusbarHtml)
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

        // const statusbarPluginRequest = this._statusbarPluginsRequestMap.get(appUrlFromStatusbarHtml)
        // // 如果队列中没有来自 app 的操作请求了 把 ipc 和 request 保存起来
        // console.log('[statusbar.main.cts]接受到了 /operation http 请求 statusbarPluginRequest === ',statusbarPluginRequest)
        // if(statusbarPluginRequest === undefined || statusbarPluginRequest.length === 0) {
          
        //   this._statusbarHtmlRequestMap.set(appUrlFromStatusbarHtml, {ipc: ipc, request: request, appUrl: appUrlFromStatusbarHtml})
        //   console.log('[statusbar.main.cts]接受到了 /operation http 请求 statusbarPluginRequest === undefined 所以保存起来了 this._operationWaitResponses ===',this._operationWaitResponses)
        //   return  ;
        // }

        // // 如果队列中有 来自 app de 操作请求
        // if(statusbarPluginRequest !== undefined && statusbarPluginRequest.length !== 0 ){
        //   ipc.postMessage(
        //     await IpcResponse.fromStream(
        //       request.req_id,
        //       200,
        //       statusbarPluginRequest.shift()?.body as ReadableStream<Uint8Array>,
        //       new IpcHeaders({
        //         "Content-type": "application/json"
        //       }),
        //       ipc
        //     )
        //   )
        //   return ;
        // }
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

        const appUrlFromApp = request.parsed_url.searchParams.get("app_url")
        console.log('[statusbar.main.cts 接受到了 /operation 操作 appUrlFromApp]', appUrlFromApp)
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

        console.log("-------------------------------------------------------------- 返回了", result)
        return result;









        // if(statusbarHtmlRequest === undefined){ // 这里是有问题的
        //   // 没有匹配的 表示之前statusbar.html 发来的操作请求已经返回了 需要把 第三方发过来的操作请求保存起来
        //   let statusbarPluginRequest = this._operationQueue.get(appUrlFromApp)
        //   const result = await new Promise(resolve => {
        //     if(statusbarPluginRequest === undefined) {
        //       this._operationQueue.set(appUrlFromApp, [{
        //         body: request.body as ReadableStream<Uint8Array>,
        //         callback: () => {
        //           resolve(true)
        //         },
        //         id: this._allocId++
        //       }])
        //     }else{
        //       statusbarPluginRequest.push({
        //         body: request.body as ReadableStream<Uint8Array>,
        //         callback: () => {
        //           resolve(true)
        //         },
        //         id: this._allocId++
        //       })
        //     } 
        //   })
        //   return true;
        // }

        // // 如果还有有 statusbar.html 发送过来的操作请求 直接把请求转发给 statubar.html
        // console.log('[statusbar.main.cts /operation 操作 向statusbar.html 发送了消息 ]', )
        // statusbarHtmlRequest.ipc.postMessage(
        //   await IpcResponse.fromStream(
        //     statusbarHtmlRequest.request.req_id,
        //     200,
        //     request.body as ReadableStream<Uint8Array>,
        //     new IpcHeaders({
        //       "Content-type": "application/json"
        //     }),
        //     statusbarHtmlRequest.ipc
        //   )
        // )
        // this._operationWaitResponses.delete(appUrlFromApp)

        // // 如果statusbar.plugin 发送过来的请求是action === get_style 那么需要等待 statusbar.html 把style值发送过来在返回
      

        // return true;
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

 
