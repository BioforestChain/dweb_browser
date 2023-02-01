"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.JsProcessManagerNMM = void 0;
const micro_module_native_1 = require("../core/micro-module.native");
/**
 * 将指定的js运行在后台的一个管理器，
 * 注意它们共享一个域，所以要么就关闭
 */
class JsProcessManagerNMM extends micro_module_native_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        this.mmid = `js.sys.dweb`;
        // static singleton = once(() => new JsProcessManager());
    }
    async _bootstrap() {
        const window = (this.window = nw.Window.open("../../js-process.html"));
        console.log(window.postMessage);
        /// 创建 web worker
        this.registerCommonIpcOnMessageHanlder({
            pathname: "/create-process",
            matchMode: "full",
            input: { main_code: "string" },
            output: "number",
            hanlder: (args) => {
                return this.createProcess(window, args.main_code);
            },
        });
    }
    async _shutdown() { }
    /** 创建一个虚拟进程 */
    createProcess(window, main_code) {
        const createWorker = () => { };
        window.eval(null, `
       
        const worker = new Worker("data:utf-8,${JSON.stringify(main_code).slice(1, -1)}")
      
      `);
        return 1;
    }
}
exports.JsProcessManagerNMM = JsProcessManagerNMM;
