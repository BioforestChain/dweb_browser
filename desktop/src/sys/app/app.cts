//  app.sys.dweb 负责启动 第三方应用程序
const fsPromises = require("fs/promises")
const path = require('path')
const process = require('process')
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
// import type { $MMID } from "../../helper/types.cjs";
 
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs"
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
 
import { resolveToRootFile } from "../../helper/createResolveTo.cjs";
import { JsMicroModule } from "../../sys/micro-module.js.cjs";
import type { DnsNMM } from "../dns/dns.cjs"


// 运行在 node 环境 
export class AppNMM extends NativeMicroModule {
    mmid = "app.sys.dweb" as const;

    constructor(
         
    ){
        super()
    }

    async _bootstrap() {
        // console.log('---------------------启动了 app.sys.dweb 应用')
      //   获取全部的 appsInfo
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/install",
            matchMode: "full",
            input: {},
            output: "boolean",
            hanlder: async (args,client_ipc, request) => {
                // 需要动态的创建一个 jsMM 
                // 启动它一个 http-server 服务器 只想对应的目录？？？如何注入 js 文件？？？
                // 必须要启动一个 jsMM 类似于 browser.main.cts 那样的
                // console.log('打开指定应用: request: ', request.url)
                const _url = new URL(request.url)
                let appId = _url.searchParams.get("appId")
                if(appId === null) return false;
                // 注意全部需要小写
                const mmid = `${appId.toLocaleLowerCase()}.app.dweb` as $MMID
                await this.fetch(`file://dns.sys.dweb/install-js?app_id=${mmid}`)
                return IpcResponse.fromText(
                    request.req_id,
                    200,
                    "ok",
                    new IpcHeaders({
                        "Content-Type": "text/json"
                    })
                )
                // return new Promise(async (resolve, reject) => {
                //     const result = await this.fetch(`file://dns.sys.dweb/install-js?app_id=${mmid}error`)
                //     console.log('------------------------- 安装失败的返回：', result)
                //     resolve(true)
                //                             // .then(async (res) => {
                //                             //     const str = await res.text()
                //                             //     console.log('成功安装了 应用： ', mmid )
                //                             //     if(str === "ok") return 
                //                             //     // this.fetch(`file://dns.sys.dweb/open?app_id=${mmid}`) 
                //                             // })
                //                             // .catch(err => {
                //                             //     console.log('err:', err)
                //                             // })
                    
                //     // IpcResponse.fromText()
                // })
                
           
            },
        });;

        // 专门用来做静态服务
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/server",
            matchMode: "full",
            input: {},
            output: "boolean",
            hanlder: async (args,client_ipc, request) => {
               console.log('app.cts 接受到了 /server: ', args, request)
            //    把需要的文件内容返回去就可以了
                return true;
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
  