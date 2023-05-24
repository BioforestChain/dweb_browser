"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.$messagePackToIpcMessage = void 0;
const msgpack_1 = require("@msgpack/msgpack");
const _messageToIpcMessage_js_1 = require("./$messageToIpcMessage.js");
const $messagePackToIpcMessage = (data, ipc) => {
    return (0, _messageToIpcMessage_js_1.$messageToIpcMessage)((0, msgpack_1.decode)(data), ipc);
};
exports.$messagePackToIpcMessage = $messagePackToIpcMessage;
