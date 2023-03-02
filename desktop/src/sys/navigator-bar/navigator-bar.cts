// 模拟状态栏模块-用来提供状态UI的模块
import fsPromises from "node:fs/promises"
import path from "node:path"
import process from "node:process";
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { createHttpDwebServer } from "../http-server/$listenHelper.cjs";
import chalk from "chalk";
 
import type { $NativeWindow } from "../../helper/openNativeWindow.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { Remote } from "comlink";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs"

// @ts-ignore
type $APIS = typeof import("./assets/multi-webview.html.mjs")["APIS"];
export class NavigatorbarNMM extends NativeMicroModule {
    mmid = "navigatorbar.sys.dweb" as const;
    private _uid_wapis_map = new Map<number,{ nww: $NativeWindow; apis: Remote<$APIS> }>();

    private _close_dweb_server?: () => unknown;
    // html 发起请求等待返回的map
    private _htmlRequestMap = new Map<string, $HtmlRequest>() 
    // plugins 发起请求的map
    private _pluginsReqestMap = new Map<string, $PluginsRequestQueueItem[]>()              
    // plugins 发起请求却还没有返回的map
    private _pluginsNoReleaseRequestMap = new Map<string, $PluginsRequestQueueItem[]>()  
    // 用来分配的ID
    private _allocId = 0;
    // 必须要通过这样启动才可以
    
    async _bootstrap() {
        console.log(chalk.green('[navigatorbar.sys.dweb 启动了]'))
        const { origin, listen, close } = await createHttpDwebServer(this, {});
        this._close_dweb_server = close;
        /// 从本地文件夹中读取数据返回，
        /// 如果是Android，则使用 AssetManager API 读取文件数据，并且需要手动绑定 mime 与 statusCode
        ;(await listen()).onRequest(async (request, ipc) => {
            // 监听 http:// 协议的请求
            // 通过 fetch("http://navigatorbar.sys.dweb-80.localhost:22605/") 访问的请求会被发送到这个位置

            const pathname = request.parsed_url.pathname;
            switch(pathname){
                case (pathname === "/" || pathname === "/index.html" ? pathname : "**eot**"):  onRequestAtIndexHtml.bind(this)(request, ipc); break;
                case "/operation_return": onRequestatOperationReturn.bind(this)(request, ipc); break;
                case "/operation_from_html": onRequestOperationFromHtml.bind(this)(request, ipc); break;
                default: () => {
                    console.log(chalk.red('[navigator-bar.cts 接受到了 没有注册事件监听器的请求]'), request)
                }
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
            input: {},
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
                console.log('[navigatorbar.main.cts 接受到了 /operation 操作 appUrlFromApp]', appUrlFromApp)
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
                
                // 把请求保存到队列
                // 调用这个执行发送国的函数 执行发送的函数必须是await 
                // 等待这个调用的函数执行完毕后在返回？？
                let statusbarPluginRequest = this._pluginsReqestMap.get(appUrlFromApp)
                const result = await new Promise<IpcResponse>(resolve => {
                    if(statusbarPluginRequest === undefined){
                        statusbarPluginRequest = []
                        this._pluginsReqestMap.set(appUrlFromApp, statusbarPluginRequest)
                    }
                    statusbarPluginRequest.push({
                        body: request.body as ReadableStream<Uint8Array>,
                        callback: (reponse: IpcResponse) => {
                            resolve(reponse)
                        },
                        req_id: request.req_id,
                        id: `${this._allocId++}` // 给来之 plugins 的添加一个标识
                    })
                    
                    // 执行发送的函数
                    this._sendToStatusbarHtml(appUrlFromApp)
                })

                return result;
            }
        })
    }

    /**
     * 
     * 发送消息给 html
     * @param appUrl 
     * @returns 
     */
    private async _sendToStatusbarHtml(appUrl: string){
        const htmlRequest = this._htmlRequestMap.get(appUrl)
        const statusbarPluginRequest = this._pluginsReqestMap.get(appUrl)
        let pluginsNoReleaseRequest = this._pluginsNoReleaseRequestMap.get(appUrl)

        if(htmlRequest === undefined) return ;
        if(statusbarPluginRequest === undefined) return;
        const operationQueueItem = statusbarPluginRequest.shift();
        if(operationQueueItem === undefined) return;
        if(pluginsNoReleaseRequest === undefined){
            pluginsNoReleaseRequest = [];
            this._pluginsNoReleaseRequestMap.set(appUrl, pluginsNoReleaseRequest)
            
        }
        pluginsNoReleaseRequest.push(operationQueueItem)

        htmlRequest.ipc.postMessage(
            await IpcResponse.fromStream(
                htmlRequest.request.req_id,
                200,
                operationQueueItem.body as ReadableStream<Uint8Array>,
                new IpcHeaders({
                    "Content-type": "application/json",
                    "id": operationQueueItem.id,
                }),
                htmlRequest.ipc
            )
        )
        // 需要删除map里面保存的数据 如果不删除可能导致 多次来至 statusbar.plugins 发送过来请求，会使用
        // 同一个  htmlRequest 发生错误
        this._htmlRequestMap.delete(appUrl);
    }


    _shutdown() {
        this._uid_wapis_map.forEach((wapi) => {
            wapi.nww.close();
        });
        this._uid_wapis_map.clear();
        this._close_dweb_server?.();
        this._close_dweb_server = undefined;
    }
}

/**
 * 通过 fetch("http://navigatorbar.sys.dweb-80.localhost:22605/") 请求的事件监听器
 * pathname === "/" || pathname === "/index.html"
 * @param request 
 * @param ipc 
 */
async function onRequestAtIndexHtml(request: IpcRequest, ipc: Ipc){
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
}

/**
 * 通过 fetch("http://navigatorbar.sys.dweb-80.localhost:22605/") 请求的事件监听器
 * pathname === "/operation_return"  
 * @param request 
 * @param ipc 
 * @param _pluginsNoReleaseRequestMap 
 * @returns 
 */
async function onRequestatOperationReturn(
    request: IpcRequest,
    ipc: Ipc,
){
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

    // @ts-ignore
    let pluginsNoReleaseRequest = this._pluginsNoReleaseRequestMap.get(appUrlFromStatusbarHtml) as $PluginsRequestQueueItem[]
    console.log("appUrlFromStatusbarHtml:", appUrlFromStatusbarHtml)
    // @ts-ignore
    console.log("this._pluginsNoReleaseRequestMap:", this._pluginsNoReleaseRequestMap)
    let itemIndex = pluginsNoReleaseRequest.findIndex(_item => _item.id === id)
    let item = pluginsNoReleaseRequest[itemIndex]
                pluginsNoReleaseRequest.splice(itemIndex, 1)
    // 返回的就是一个 json
    const data = await readStream(request.body as ReadableStream)
    console.log('[navigator-bar.cts onRequestatOperationReturn item]', item)
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

/**
 * 通过 fetch("http://navigatorbar.sys.dweb-80.localhost:22605/") 请求的事件监听器
 * pathname === "/operation_from_html"  
 * @param request 
 * @param ipc 
 * @param _pluginsNoReleaseRequestMap 
 * @returns 
 */
async function onRequestOperationFromHtml(
    request: IpcRequest,
    ipc: Ipc,
){
    console.log(chalk.green('[navigator-bar.cts 接受到了 html 发送过来的等待操作的请求]'))
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
    // @ts-ignore
    this._htmlRequestMap.set(appUrlFromStatusbarHtml, {ipc: ipc, request: request, appUrl: appUrlFromStatusbarHtml})
    // @ts-ignore
    this._sendToStatusbarHtml(appUrlFromStatusbarHtml)
 
}


// 读取 html 文件
async function reqadHtmlFile(){
    const targetPath = path.resolve(process.cwd(), "./assets/html/navigatorbar.html")
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

export interface $PluginsRequestQueueItem{
    body: ReadableStream<Uint8Array>
    callback: {(response: IpcResponse): void} ;
    req_id: number;
    id: string; // 队列项的标识符
}

export interface $HtmlRequest{
    ipc:Ipc;
    request: IpcRequest;
    appUrl: string;  // appUrl 标识 当前statusbar搭配的是哪个 app 显示的
}

export interface $NavigatorbarItemContent{
    id: string;
    icon: string;
    label: string;
}
