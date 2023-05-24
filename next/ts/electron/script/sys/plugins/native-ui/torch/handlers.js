"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.torchState = exports.toggleTorch = void 0;
async function toggleTorch(args, client_ipc, ipcRequest) {
    // const search = querystring.unescape(ipcRequest.url).split("?")[1]
    const host = ipcRequest.parsed_url.host;
    const pathname = ipcRequest.parsed_url.pathname;
    const search = ipcRequest.parsed_url.search;
    const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}`;
    const result = await this.nativeFetch(url);
    return result;
}
exports.toggleTorch = toggleTorch;
async function torchState(args, client_ipc, ipcRequest) {
    const host = ipcRequest.parsed_url.host;
    const pathname = ipcRequest.parsed_url.pathname;
    const search = ipcRequest.parsed_url.search;
    const url = `file://mwebview.sys.dweb/plugin/${host}${pathname}${search}`;
    const result = await this.nativeFetch(url);
    return result;
}
exports.torchState = torchState;
