//  file.sys.dweb 负责下载并管理安装包
const fsPromises = require("fs/promises");
const path = require("path");
const process = require("process");
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
// import type { $MMID } from "../../helper/types.cjs";
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import type { $State } from "./file-download.cjs";
import { download } from "./file-download.cjs";
import { getAllApps } from "./file-get-all-apps.cjs";

// 运行在 node 环境
export class FileNMM extends NativeMicroModule {
    mmid = "file.sys.dweb" as const;
    async _bootstrap() {

      let downloadProcess: number = 0;
      this.registerCommonIpcOnMessageHandler({
        pathname: "/download",
        matchMode: "full",
        input: {url: "string", app_id: "string"},
        output: "boolean",
        handler: async (arg, client_ipc, request) => {
          console.log('[file.cts]arg==',arg)
            return new Promise((resolve, reject) => {
                download(arg.url,arg.app_id, _progressCallback)
                .then((apkInfo) => {
                  resolve(IpcResponse.fromJson(request.req_id, 200, undefined, apkInfo, client_ipc))
                })
                .catch((err: Error) => {
                  resolve(IpcResponse.fromJson(request.req_id, 500, undefined, JSON.stringify(err), client_ipc))
                  console.log('下载出错： ',err)
                })
            })
           
            function _progressCallback(state: $State){
              downloadProcess = state.percent
              console.log('file.cts state.percent: ', state.percent)
            }
        },
      });

      //   获取下载的进度
      this.registerCommonIpcOnMessageHandler({
        pathname: "/download_process",
        matchMode: "full",
        input: {},
        output: "boolean",
        handler: async (args,client_ipc, request) => {
          return IpcResponse.fromText(
            request.req_id,
            200,
            new IpcHeaders({
              "Content-Type": "text/plain",
            }),
            downloadProcess + "",
            client_ipc
          )
        }
      })



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
            new IpcHeaders({
              "Content-Type": "text/json",
            }),
            JSON.stringify(appsInfo),
            client_ipc
          )
        }
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
