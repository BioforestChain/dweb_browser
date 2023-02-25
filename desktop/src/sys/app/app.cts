//  app.sys.dweb 负责启动 第三方应用程序
import fs from "node:fs"
import fsPromises from "node:fs/promises"
import path from "node:path"
import process from "node:process"
// const path = require('path')
// const process = require('process')
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
// import type { $MMID } from "../../helper/types.cjs";
 
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs"
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
 
import { resolveToRootFile } from "../../helper/createResolveTo.cjs";
import { JsMicroModule } from "../../sys/micro-module.js.cjs";
import type { DnsNMM } from "../dns/dns.cjs"
import type { $AppInfo } from "../file/file-get-all-apps.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs"


// 运行在 node 环境 
export class AppNMM extends NativeMicroModule {
    mmid = "app.sys.dweb" as const;

    constructor(
         
    ){
        super()
    }

    async _bootstrap() {
        
        //  获取全部的 appsInfo
        this.registerCommonIpcOnMessageHandler({
            pathname: "/install",
            matchMode: "full",
            input: {},
            output: "boolean",
            handler: async (args,client_ipc, request) => {
                // 需要动态的创建一个 jsMM 
                // 启动它一个 http-server 服务器 只想对应的目录？？？如何注入 js 文件？？？
                // 必须要启动一个 jsMM 类似于 browser.main.cts 那样的
                // console.log('打开指定应用: request: ', request.url)
                const _url = new URL(request.url)
                let appId = _url.searchParams.get("appId")
                if(appId === null) return false;
                // 注意全部需要小写
                const mmid = createMMIDFromAppID(appId)
                const response = await this.fetch(`file://dns.sys.dweb/install-js?app_id=${mmid}`)
                return IpcResponse.fromResponse(
                    request.req_id,
                    response,
                    client_ipc
                )
            },
        });;

         // 专门用来做静态服务
         this.registerCommonIpcOnMessageHandler({
            pathname: "/open",
            matchMode: "full",
            input: {},
            output: "boolean",
            handler: async (args,client_ipc, request) => {
                const _url = new URL(request.url)
                let appId = _url.searchParams.get("appId")
                if(appId === null) return false;
                // 注意全部需要小写
                const mmid = createMMIDFromAppID(appId)
                const response = await  this.fetch(`file://dns.sys.dweb/open?app_id=${mmid}`) ;
                console.log('[app.cts open.response]: ', response)
                return IpcResponse.fromResponse(
                    request.req_id,
                    response,
                    client_ipc
                )
            },
        });;

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
                console.log('request.parse_url: ', request.parsed_url)
                console.log('app.cts 接受到了 /server: ', args, request)
                const fromUrl = request.parsed_url.searchParams.get('url')
                if(fromUrl === null) return IpcResponse.fromText(request.req_id, 500, '缺少url查询参数')
                const targetUrl = new URL(fromUrl)
                const appId = targetUrl.hostname.split(".")[1]
                const appsInfo = JSON.parse(await (await this.fetch("file://file.sys.dweb/appsinfo")).json())
                const appInfo:$AppInfo = appsInfo.find((item: $AppInfo) => item.appId === appId)
                if(appInfo === null) return IpcResponse.fromText(request.req_id, 500, '没有找到匹配的 appId')
                console.log("[app.cts appsInfo]:", appsInfo)
                console.log("[app.cts.targetUrl]: ", targetUrl.pathname)
                switch(targetUrl.pathname){
                    case targetUrl.pathname === "/" || targetUrl.pathname === "/index.html" ? targetUrl.pathname : "**eot**" : return onServerAtIndexHtml(args,client_ipc, request, appInfo.folderName);
                    default: return onServerAtDefault(args,client_ipc, request, appInfo.folderName, targetUrl.pathname)
                }
            },
        });;
    }

    protected _shutdown(): unknown {
        throw new Error("Method not implemented.");
    }
    // _shutdown() {}
    // private register(mmid: $MMID) {
    //   /// TODO 这里应该有用户授权，允许开机启动
    // //   this.registeredMmids.add(mmid);
    //   return true;
    // }
    // private unregister(mmid: $MMID) {
    //   /// TODO 这里应该有用户授权，取消开机启动
    //   return this.registeredMmids.delete(mmid);
    // }
    // $Routers: {
    //    "/register": IO<mmid, boolean>;
    //    "/unregister": IO<mmid, boolean>;
    // };
}

/**
 * 创建 mmid 根据appId
 */
function createMMIDFromAppID(appId: string){
    return `app.${appId.toLocaleLowerCase()}.dweb` as $MMID
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
  