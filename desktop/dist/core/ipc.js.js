"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.JsIpc = void 0;
const ipc_native_1 = require("./ipc.native");
/**
 * > 在NW.js里，JsIpc几乎等价于 NativeIPC，都是使用原生的 MessagePort 即可
 * 差别只在于 JsIpc 需要传入到运作JS线程池的 WebDocument 中，传递给指定的Worker
 */
class JsIpc extends ipc_native_1.NativeIpc {
    constructor(port_id) {
        super(port);
    }
}
exports.JsIpc = JsIpc;
