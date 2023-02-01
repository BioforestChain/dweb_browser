"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.JsMicroModule = void 0;
const micro_module_cjs_1 = require("../core/micro-module.cjs");
const js_process_cjs_1 = require("./js-process.cjs");
/**
 * 所有的js程序都只有这么一个动态的构造器
 */
class JsMicroModule extends micro_module_cjs_1.MicroModule {
    constructor(mmid, 
    /**
     * js程序是动态外挂的
     * 所以需要传入一份配置信息
     */
    metadata) {
        super();
        this.mmid = mmid;
        this.metadata = metadata;
        this._connectting_ipcs = new Set();
    }
    /** 每个 JMM 启动都要依赖于某一个js */
    async _bootstrap() {
        const process_id = (this._process_id = await this.fetch(`file://js.sys.dweb/create-process?main_code=${encodeURIComponent(await fetch(this.metadata.main_url).then((res) => res.text()))}`).number());
    }
    async _connect() {
        const process_id = this._process_id;
        if (process_id === undefined) {
            throw new Error("process_id no found.");
        }
        const port_id = await this.fetch(`file://js.sys.dweb/create-ipc?process_id=${process_id}`).number();
        return new js_process_cjs_1.JsIpc(port_id);
    }
    _shutdown() {
        for (const inner_ipc of this._connectting_ipcs) {
            inner_ipc.close();
        }
        this._connectting_ipcs.clear();
    }
}
exports.JsMicroModule = JsMicroModule;
