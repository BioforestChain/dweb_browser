"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.cancel = exports.resume = exports.pause = exports.install = void 0;
/**
 * 功能：
 * 打开一个新的 webveiw 页面
 * @param this
 * @param args
 * @param client_ipc
 * @param ipcRequest
 * @returns
 */
async function install(args, client_ipc, ipcRequest) {
    // 需要同时查询参数传递进去
    if (this.wwwServer === undefined)
        throw new Error(`this.wwwServer === undefined`);
    const interUrl = this.wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
        url.pathname = "/index.html";
    }).href;
    const url = `file://mwebview.sys.dweb/open_new_webveiw_at_focused?url=${interUrl}`;
    const body = JSON.stringify({ metadataUrl: args.metadataUrl });
    await this.nativeFetch(url, {
        method: "POST",
        body
    });
    return true;
}
exports.install = install;
async function pause(args, client_ipc, ipcRequest) {
    console.log("................ 下载暂停但是还没有处理");
    return true;
}
exports.pause = pause;
async function resume(args, client_ipc, ipcRequest) {
    console.log("................ 从新下载但是还没有处理");
    return true;
}
exports.resume = resume;
// 业务逻辑是会 停止下载 立即关闭下载页面
async function cancel(args, client_ipc, ipcRequest) {
    console.log("................ 从新下载但是还没有处理");
    return true;
}
exports.cancel = cancel;
