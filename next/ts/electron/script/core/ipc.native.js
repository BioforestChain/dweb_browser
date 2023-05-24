"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.NativeIpc = void 0;
const MessagePortIpc_js_1 = require("./ipc-web/MessagePortIpc.js");
class NativeIpc extends MessagePortIpc_js_1.MessagePortIpc {
    constructor(port, remote, role) {
        super(port, remote, role);
    }
}
exports.NativeIpc = NativeIpc;
