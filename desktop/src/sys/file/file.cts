//  file.sys.dweb 负责下载并管理安装包
const fsPromises = require("fs/promises")
const path = require('path')
const process = require('process')
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
// import type { $MMID } from "../../helper/types.cjs";
import { download } from "./file-download.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs"
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { getAllApps } from "./file-get-all-apps.cjs"
import type { $State } from "./file-download.cjs";




// 运行在 node 环境 
export class FileNMM extends NativeMicroModule {
    mmid = "file.sys.dweb" as const;
    async _bootstrap() {
      this.registerCommonIpcOnMessageHandler({
        pathname: "/download",
        matchMode: "full",
        input: {url: "string"},
        output: "boolean",
        handler: async (arg, client_ipc, request) => {
            return new Promise((resolve, reject) => {
                download(arg.url, _progressCallback)
                .then((apkInfo) => {
                  resolve(IpcResponse.fromJson(request.req_id, 200, apkInfo))
                })
                .catch((err: Error) => {
                  resolve(IpcResponse.fromJson(request.req_id, 500, JSON.stringify(err)))
                  console.log('下载出错： ',err)
                })
            })
            function _progressCallback(state: $State){
                // console.log('state: ', state)
                // request.req_id:  0
                // console.log('request.req_id: ', request.req_id)
                // client_ipc.postMessage(
                //     IpcResponse.fromText(
                //         request.req_id,
                //         200,
                //         JSON.stringify(state),
                //         new IpcHeaders({
                //           "Content-Type": "text/json",
                //         })
                //     )
                // )
                // 不通过这个 下载进度先不考虑
            }

            // return true;

            // 测试地址： https://bfm-prd-download.oss-cn-hongkong.aliyuncs.com/cot/COT-beta-202302091839.apk
            // console.log('接受到了下载的信息 执行下载的程序: ', arg)
        },
      });

      //   获取全部的 appsInfo
      this.registerCommonIpcOnMessageHandler({
        pathname: "/appsinfo",
        matchMode: "full",
        input: {},
        output: "boolean",
        handler: async (args,client_ipc, request) => {
          // console.log('file.cts 开始获取 appsInfo---------------------------+++')
          const appsInfo = await getAllApps()
          // console.log('file.cts 获取到了 appsInfo---------------------------+++appsInfo： ', appsInfo)
          // console.log("JSON.stringify(appsInfo): ",JSON.stringify(appsInfo))
          return IpcResponse.fromJson(
            request.req_id,
            200,
            JSON.stringify(appsInfo),
            new IpcHeaders({
              "Content-Type": "text/json",
            })
          )
        },
      });

      //   获取全部的 icon
      this.registerCommonIpcOnMessageHandler({
        pathname: "/icon",
        matchMode: "full",
        input: {},
        output: "boolean",
        handler: async (args,client_ipc, request) => {

          // console.log('file.cts /icon request: ', args, client_ipc, request, )
          const url = new URL(request.url)
          const searchParams = url.searchParams
          // console.log('searchParams: ', searchParams)
          const appId = searchParams.get('appId')
          const name = searchParams.get("name")
          const _path = path.resolve(process.cwd(), `./apps/${appId}/sys/assets/react.svg`)
          const svgStr = await fsPromises.readFile(_path, {encoding: "utf8"})
          return IpcResponse.fromText(
            request.req_id,
            200,
            svgStr,
            new IpcHeaders({
              "Content-Type": "image/svg+xml",
            })
          )
        },
      });
  
    //   /// 开始启动开机项
    //   for (const mmid of this.registeredMmids) {
    //     /// TODO 这里应该使用总线进行通讯，而不是拿到core直接调用。在未来分布式系统中，core模块可能是远程模块
    //     this.fetch(`file://dns.sys.dweb/open?app_id=${mmid}`);
    //     //  await core.open(mmid);
    //   }
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
  