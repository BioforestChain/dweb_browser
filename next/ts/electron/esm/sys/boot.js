import { NativeMicroModule } from "../core/micro-module.native.js";
export class BootNMM extends NativeMicroModule {
    constructor(initMmids) {
        super();
        Object.defineProperty(this, "initMmids", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: initMmids
        });
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "boot.sys.dweb"
        });
        // private registeredMmids = new Set<$MMID>(["desktop.sys.dweb"]); // 被优化
        Object.defineProperty(this, "registeredMmids", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Set(this.initMmids)
        });
    }
    async _bootstrap() {
        this.registerCommonIpcOnMessageHandler({
            pathname: "/register",
            matchMode: "full",
            input: {},
            output: "boolean",
            handler: async (args, ipc) => {
                return await this.register(ipc.remote.mmid);
            },
        });
        this.registerCommonIpcOnMessageHandler({
            pathname: "/unregister",
            matchMode: "full",
            input: {},
            output: "boolean",
            handler: async (args, ipc) => {
                return await this.unregister(ipc.remote.mmid);
            },
        });
        /// 开始启动开机项
        for (const mmid of this.registeredMmids) {
            /// TODO 这里应该使用总线进行通讯，而不是拿到core直接调用。在未来分布式系统中，core模块可能是远程模块
            await this.nativeFetch(`file://dns.sys.dweb/open?app_id=${mmid}`);
        }
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
