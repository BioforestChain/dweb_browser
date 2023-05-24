import { IpcResponse } from "../../core/ipc/index.js";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.js";
export async function createWWWServer() {
    // 为 下载页面做 准备
    this.wwwServer = await createHttpDwebServer(this, {
        subdomain: "www",
        port: 6363
    });
    const wwwReadableStreamIpc = await this.wwwServer.listen();
    wwwReadableStreamIpc.onRequest(onRequest.bind(this));
}
async function onRequest(request, ipc) {
    let pathname = request.parsed_url.pathname;
    pathname = pathname === "/" ? "/index.html" : pathname;
    const url = `file:///assets/page_download/${pathname}?mode=stream`;
    // 打开首页的 路径
    const response = await this.nativeFetch(url);
    ipc.postMessage(await IpcResponse.fromResponse(request.req_id, response, ipc));
}
