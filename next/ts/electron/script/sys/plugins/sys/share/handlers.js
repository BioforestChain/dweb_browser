"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.createStreamIpc = exports.share = void 0;
const ReadableStreamIpc_js_1 = require("../../../../core/ipc-web/ReadableStreamIpc.js");
const index_js_1 = require("../../../../core/ipc/index.js");
/**
 * 设置状态
 * @param this
 * @param args
 * @param client_ipc
 * @param ipcRequest
 * @returns
 */
async function share(args, client_ipc, ipcRequest) {
    const host = ipcRequest.parsed_url.host;
    const pathname = ipcRequest.parsed_url.pathname;
    const search = ipcRequest.parsed_url.search;
    const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}`;
    const result = await this.nativeFetch(url, {
        body: ipcRequest.body.raw,
        headers: ipcRequest.headers,
        method: ipcRequest.method,
    });
    return result;
}
exports.share = share;
async function createStreamIpc(args, client_ipc, ipcRequest) {
    const readableStreamIpcToTestFromSysDweb = new ReadableStreamIpc_js_1.ReadableStreamIpc(this, index_js_1.IPC_ROLE.SERVER);
    readableStreamIpcToTestFromSysDweb.bindIncomeStream(ipcRequest.body.stream());
    readableStreamIpcToTestFromSysDweb.onEvent(event => {
        console.log('event: ', event);
    });
    readableStreamIpcToTestFromSysDweb.onStream(stream => {
        console.log('stream: ', stream.binary);
    });
    return readableStreamIpcToTestFromSysDweb.stream;
}
exports.createStreamIpc = createStreamIpc;
