"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.createWWWServer = void 0;
const index_js_1 = require("../../core/ipc/index.js");
const _createHttpDwebServer_js_1 = require("../http-server/$createHttpDwebServer.js");
async function createWWWServer() {
    // 为 下载页面做 准备
    this.wwwServer = await (0, _createHttpDwebServer_js_1.createHttpDwebServer)(this, {
        subdomain: "www",
        port: 6363
    });
    const wwwReadableStreamIpc = await this.wwwServer.listen();
    wwwReadableStreamIpc.onRequest(onRequest.bind(this));
}
exports.createWWWServer = createWWWServer;
async function onRequest(request, ipc) {
    let pathname = request.parsed_url.pathname;
    pathname = pathname === "/" ? "/index.html" : pathname;
    const url = `file:///assets/page_download/${pathname}?mode=stream`;
    // 打开首页的 路径
    const response = await this.nativeFetch(url);
    ipc.postMessage(await index_js_1.IpcResponse.fromResponse(request.req_id, response, ipc));
}
