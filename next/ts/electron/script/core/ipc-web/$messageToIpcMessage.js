"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.$jsonToIpcMessage = exports.$messageToIpcMessage = exports.$objectToIpcMessage = exports.$isIpcSignalMessage = void 0;
const const_js_1 = require("../ipc/const.js");
const IpcBodyReceiver_js_1 = require("../ipc/IpcBodyReceiver.js");
const IpcEvent_js_1 = require("../ipc/IpcEvent.js");
const IpcHeaders_js_1 = require("../ipc/IpcHeaders.js");
const IpcRequest_js_1 = require("../ipc/IpcRequest.js");
const IpcResponse_js_1 = require("../ipc/IpcResponse.js");
const IpcStreamAbort_js_1 = require("../ipc/IpcStreamAbort.js");
const IpcStreamData_js_1 = require("../ipc/IpcStreamData.js");
const IpcStreamEnd_js_1 = require("../ipc/IpcStreamEnd.js");
const IpcStreamPaused_js_1 = require("../ipc/IpcStreamPaused.js");
const IpcStreamPulling_js_1 = require("../ipc/IpcStreamPulling.js");
const MetaBody_js_1 = require("../ipc/MetaBody.js");
const $isIpcSignalMessage = (msg) => msg === "close" || msg === "ping" || msg === "pong";
exports.$isIpcSignalMessage = $isIpcSignalMessage;
const $objectToIpcMessage = (data, ipc) => {
    let message;
    if (data.type === const_js_1.IPC_MESSAGE_TYPE.REQUEST) {
        message = new IpcRequest_js_1.IpcRequest(data.req_id, data.url, data.method, new IpcHeaders_js_1.IpcHeaders(data.headers), new IpcBodyReceiver_js_1.IpcBodyReceiver(MetaBody_js_1.MetaBody.fromJSON(data.metaBody), ipc), ipc);
    }
    else if (data.type === const_js_1.IPC_MESSAGE_TYPE.RESPONSE) {
        message = new IpcResponse_js_1.IpcResponse(data.req_id, data.statusCode, new IpcHeaders_js_1.IpcHeaders(data.headers), new IpcBodyReceiver_js_1.IpcBodyReceiver(MetaBody_js_1.MetaBody.fromJSON(data.metaBody), ipc), ipc);
    }
    else if (data.type === const_js_1.IPC_MESSAGE_TYPE.EVENT) {
        message = new IpcEvent_js_1.IpcEvent(data.name, data.data, data.encoding);
    }
    else if (data.type === const_js_1.IPC_MESSAGE_TYPE.STREAM_DATA) {
        message = new IpcStreamData_js_1.IpcStreamData(data.stream_id, data.data, data.encoding);
    }
    else if (data.type === const_js_1.IPC_MESSAGE_TYPE.STREAM_PULLING) {
        message = new IpcStreamPulling_js_1.IpcStreamPulling(data.stream_id, data.bandwidth);
    }
    else if (data.type === const_js_1.IPC_MESSAGE_TYPE.STREAM_PAUSED) {
        message = new IpcStreamPaused_js_1.IpcStreamPaused(data.stream_id, data.fuse);
    }
    else if (data.type === const_js_1.IPC_MESSAGE_TYPE.STREAM_ABORT) {
        message = new IpcStreamAbort_js_1.IpcStreamAbort(data.stream_id);
    }
    else if (data.type === const_js_1.IPC_MESSAGE_TYPE.STREAM_END) {
        message = new IpcStreamEnd_js_1.IpcStreamEnd(data.stream_id);
    }
    return message;
};
exports.$objectToIpcMessage = $objectToIpcMessage;
const $messageToIpcMessage = (data, ipc) => {
    if ((0, exports.$isIpcSignalMessage)(data)) {
        return data;
    }
    return (0, exports.$objectToIpcMessage)(data, ipc);
};
exports.$messageToIpcMessage = $messageToIpcMessage;
const $jsonToIpcMessage = (data, ipc) => {
    if ((0, exports.$isIpcSignalMessage)(data)) {
        return data;
    }
    return (0, exports.$objectToIpcMessage)(JSON.parse(data), ipc);
};
exports.$jsonToIpcMessage = $jsonToIpcMessage;
