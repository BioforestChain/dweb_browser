"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.BootNMM = void 0;
const micro_module_native_cjs_1 = require("../core/micro-module.native.cjs");
class BootNMM extends micro_module_native_cjs_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        this.mmid = "boot.sys.dweb";
        this.registeredMmids = new Set(["desktop.sys.dweb"]);
        // $Routers: {
        //    "/register": IO<mmid, boolean>;
        //    "/unregister": IO<mmid, boolean>;
        // };
    }
    async _bootstrap() {
        for (const mmid of this.registeredMmids) {
            /// TODO 这里应该使用总线进行通讯，而不是拿到core直接调用。在未来分布式系统中，core模块可能是远程模块
            this.fetch(`file://dns.sys.dweb/open?app_id=${mmid}`);
            //  await core.open(mmid);
        }
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/register",
            matchMode: "full",
            input: { app_id: "mmid" },
            output: "boolean",
            hanlder: async (args) => {
                return await this.register(args.app_id);
            },
        });
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/unregister",
            matchMode: "full",
            input: { app_id: "mmid" },
            output: "boolean",
            hanlder: async (args) => {
                return await this.unregister(args.app_id);
            },
        });
    }
    _shutdown() { }
    register(mmid) {
        /// TODO 这里应该有用户授权，允许开机启动
        this.registeredMmids.add(mmid);
        return true;
    }
    unregister(mmid) {
        /// TODO 这里应该有用户授权，取消开机启动
        return this.registeredMmids.delete(mmid);
    }
}
exports.BootNMM = BootNMM;
