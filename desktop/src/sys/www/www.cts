// www.sys.dweb 模块用来提供静态服务
// 第三方程序需要的资源文件全部通过这个模块实现
import fsPromises from "node:fs/promises"
import path from "node:path"
import process from "node:process"
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs"
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import type { $AppInfo } from "../file/file-get-all-apps.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs"

export class WWWNMM extends NativeMicroModule{
    mmid = "www.sys.dweb" as const;

    async _bootstrap(){
        // 注册通过 jsProcess 发送过来的访问请求
        // 专门用来做静态服务
        this.registerCommonIpcOnMessageHandler({
            pathname: "/server",
            matchMode: "full",
            input: {},
            output: "boolean",
            handler: async (args,client_ipc, request) => {
                // 在返回首页的时候需要注入 dweb-top-bar 这样的自定义组件, 要实现功能的访问
                //把需要的文件内容返回去就可以了
                // request.url === 'file://app.sys.dweb/server?url=http://app.w85defe5.dweb-80.localhost:22605/'
                const fromUrl = request.parsed_url.searchParams.get('url')
                if(fromUrl === null) return IpcResponse.fromText(request.req_id, 500, '缺少url查询参数')
                const targetUrl = new URL(fromUrl)
                const appId = targetUrl.hostname.split(".")[1]
                const appsInfo = JSON.parse(await (await this.fetch("file://file.sys.dweb/appsinfo")).json())
                const appInfo:$AppInfo = appsInfo.find((item: $AppInfo) => item.appId === appId)
                if(appInfo === null) return IpcResponse.fromText(request.req_id, 500, '没有找到匹配的 appId')
                switch(targetUrl.pathname){
                    case targetUrl.pathname === "/" || targetUrl.pathname === "/index.html" ? targetUrl.pathname : "**eot**" : return onServerAtIndexHtml(args,client_ipc, request, appInfo.folderName);
                    // 非首页直接读取文件 的内容返回
                    default: return onServerAtDefault(args,client_ipc, request, appInfo.folderName, targetUrl.pathname)
                }
            },
        });;

    }

    protected _shutdown(): unknown {
        throw new Error("Method not implemented.");
    }
}

/**
 * /server 查询 index.html 事件处理器
 */
async function onServerAtIndexHtml(args: unknown, client_ipc: Ipc, ipc_request: IpcRequest, folderName: string){
    const targetPath = path.resolve(process.cwd(), `./apps/${folderName}/sys/index.html`)
    const content = await fsPromises.readFile(targetPath)
    console.log("[app.cts onServerAtIndexHtml]:", content)
    return IpcResponse.fromBinary(
        ipc_request.req_id,
        200,
        content,
        new IpcHeaders({ "Content-Type": "text/html" }),
        client_ipc
    )
}

/**
 * /server 查询 default 事件处理器
 */
async function onServerAtDefault(args: unknown, client_ipc: Ipc, ipc_request: IpcRequest, folderName: string, targetPath: string){
    const _targetPath = path.resolve(process.cwd(), `./apps/${folderName}/sys/${targetPath}`)
    console.log('_targetPath: ', _targetPath)
    const content = await fsPromises.readFile(_targetPath)
    console.log("[app.cts onServerAtDefault]:", content)
    return IpcResponse.fromBinary(
        ipc_request.req_id,
        200,
        content,
        new IpcHeaders({ 
            "Content-Type": createContentType(_targetPath) 
        }),
        client_ipc
    )
}

const allMIME: { [key: string]: string} = {
    // text 类型
    html:"text/html",
    css:"text/css",
    js:"text/javascript",
    // 图片类型
    apng:"image/apng",
    avif:"image/avif",
    bmp:"image/bmp",
    gif:"image/gif",
    ico:"image/x-icon",
    cur:"image/x-icon",
    jpg:"image/jpeg",
    jpeg:"image/jpeg",
    jfif:"image/jpeg",
    pjpeg:"image/jpeg",
    pjp:"image/jpeg",
    png:"image/png",
    svg:"image/svg+xml",
    tif:"image/tiff",
    tiff:"image/tiff",
    webp:"image/webp",
    // 未完待续
    // "":"",
    // "":"",
    // "":"",
    // "":"",
    // "":"",
    // "":"",
    // "":"",
    // "":"",
}

function createContentType(path: string){
    const end = path.split(".").reverse()[0]
    console.log("[app.cts onServerAtDefault]:", path, path.split("."), end, allMIME[end])
    return allMIME[end]
}