//  app.sys.dweb 负责启动 第三方应用程序
 
import fsPromises from "node:fs/promises"
import path from "node:path"
import process from "node:process"
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs"
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import type { $AppInfo } from "../file/file-get-all-apps.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs"


// 运行在 node 环境 
export class AppNMM extends NativeMicroModule {
    mmid = "app.sys.dweb" as const;

    async _bootstrap() {
        
        //  安装 第三方 app
        this.registerCommonIpcOnMessageHandler({
            pathname: "/install",
            matchMode: "full",
            input: {},
            output: "boolean",
            handler: async (args,client_ipc, request) => {
                // 需要动态的创建一个 jsMM 
                // 启动它一个 http-server 服务器 只想对应的目录？？？如何注入 js 文件？？？
                // 必须要启动一个 jsMM 类似于 browser.main.cts 那样的
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
                return IpcResponse.fromResponse(
                    request.req_id,
                    response,
                    client_ipc
                )
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
