"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.FileNMM = void 0;
//  file.sys.dweb 负责下载并管理安装包
const micro_module_native_js_1 = require("../../core/micro-module.native.js");
// import type { $MMID } from "../../helper/types.ts";
const IpcHeaders_js_1 = require("../../core/ipc/IpcHeaders.js");
const IpcResponse_js_1 = require("../../core/ipc/IpcResponse.js");
const file_download_js_1 = require("./file-download.js");
const file_get_all_apps_js_1 = require("./file-get-all-apps.js");
const chalk_1 = __importDefault(require("chalk"));
// 运行在 node 环境
class FileNMM extends micro_module_native_js_1.NativeMicroModule {
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
                console.log(chalk_1.default.red("file.cts /download appInfo === ", appInfo));
                return new Promise((resolve, reject) => {
                    (0, file_download_js_1.download)(arg.url, arg.app_id, _progressCallback, appInfo)
                        .then((apkInfo) => {
                        resolve(IpcResponse_js_1.IpcResponse.fromJson(request.req_id, 200, undefined, apkInfo, client_ipc));
                    })
                        .catch((err) => {
                        resolve(IpcResponse_js_1.IpcResponse.fromJson(request.req_id, 500, undefined, JSON.stringify(err), client_ipc));
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
                return IpcResponse_js_1.IpcResponse.fromText(request.req_id, 200, new IpcHeaders_js_1.IpcHeaders({
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
                const appsInfo = await (0, file_get_all_apps_js_1.getAllApps)();
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
exports.FileNMM = FileNMM;
