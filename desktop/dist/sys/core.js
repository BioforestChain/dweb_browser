"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AppNMM = void 0;
/** 内核，原 DNS 服务 */
class AppNMM extends NativeMicroModule {
    constructor() {
        super(...arguments);
        this.mmid = "core.sys.dweb";
        this.apps = new Map();
        this.running = new Map();
    }
    bootstrap() {
        // const parseArgs_open = $parseRequestToParams({ app_id: "mmid" });
        // const parseArgs_close = $parseRequestToParams({ app_id: "mmid" });
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/install-js",
            matchMode: "full",
            input: {},
            output: "void",
            hanlder: () => {
                /// TODO 动态创建 JsMicroModule
            },
        });
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/open",
            matchMode: "full",
            input: { app_id: "mmid" },
            output: "boolean",
            hanlder: async (args) => {
                /// TODO 询问用户是否授权该行为
                await this.open(args.app_id);
                return true;
            },
        });
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/close",
            matchMode: "full",
            input: { app_id: "mmid" },
            output: "boolean",
            hanlder: async (args) => {
                /// TODO 关闭应用首先要确保该应用的 parentProcessId 在 processTree 中
                await this.close(args.app_id);
                return true;
            },
        });
    }
    async destroy() {
        for (const mmid of this.running.keys()) {
            await this.close(mmid);
        }
    }
    /** 查询应用 */
    async query(mmid) {
        return this.apps.get(mmid);
    }
    /** 打开应用 */
    async open(mmid) {
        let app = this.running.get(mmid);
        if (app === undefined) {
            const MM = await this.query(mmid);
            if (MM === undefined) {
                throw new Error(`no found app: ${mmid}`);
            }
            app = new MM();
            this.running.get(mmid);
        }
        return app;
    }
    /** 关闭应用 */
    async close(mmid) {
        const app = this.running.get(mmid);
        if (app === undefined) {
            return -1;
        }
        try {
            await app.destroy();
            return 0;
        }
        catch {
            return 1;
        }
    }
}
exports.AppNMM = AppNMM;
