//  file.sys.dweb 负责下载并管理安装包
import { NativeMicroModule } from "../../core/micro-module.native.js";
// import type { $MMID } from "../../helper/types.ts";
import { IpcHeaders } from "../../core/ipc/IpcHeaders.js";
import { IpcResponse } from "../../core/ipc/IpcResponse.js";
import { download } from "./file-download.js";
import { getAllApps } from "./file-get-all-apps.js";
import chalk from "chalk";
// 运行在 node 环境
export class FileNMM extends NativeMicroModule {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "file.sys.dweb"
        });
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
    async _bootstrap() {
        let downloadProcess = 0;
        this.registerCommonIpcOnMessageHandler({
            method: "POST",
            pathname: "/download",
            matchMode: "full",
            input: { url: "string", app_id: "string" },
            output: "boolean",
            handler: async (arg, client_ipc, request) => {
                const appInfo = await request.body.text();
                console.log(chalk.red("file.cts /download appInfo === ", appInfo));
                return new Promise((resolve, reject) => {
                    download(arg.url, arg.app_id, _progressCallback, appInfo)
                        .then((apkInfo) => {
                        resolve(IpcResponse.fromJson(request.req_id, 200, undefined, apkInfo, client_ipc));
                    })
                        .catch((err) => {
                        resolve(IpcResponse.fromJson(request.req_id, 500, undefined, JSON.stringify(err), client_ipc));
                        console.log("下载出错： ", err);
                    });
                });
                function _progressCallback(state) {
                    downloadProcess = state.percent;
                }
            },
        });
        //   获取下载的进度
        this.registerCommonIpcOnMessageHandler({
            pathname: "/download_process",
            matchMode: "full",
            input: {},
            output: "boolean",
            handler: async (args, client_ipc, request) => {
                return IpcResponse.fromText(request.req_id, 200, new IpcHeaders({
                    "Content-Type": "text/plain",
                }), downloadProcess + "", client_ipc);
            },
        });
        //   获取全部的 appsInfo
        this.registerCommonIpcOnMessageHandler({
            pathname: "/appsinfo",
            matchMode: "full",
            input: {},
            output: "object",
            handler: async (args, client_ipc, request) => {
                const appsInfo = await getAllApps();
                return appsInfo;
            },
        });
        //   /// 开始启动开机项
        //   for (const mmid of this.registeredMmids) {
        //     /// TODO 这里应该使用总线进行通讯，而不是拿到core直接调用。在未来分布式系统中，core模块可能是远程模块
        //     this.fetch(`file://dns.sys.dweb/open?app_id=${mmid}`);
        //     //  await core.open(mmid);
        //   }
    }
    _shutdown() {
        throw new Error("Method not implemented.");
    }
}
