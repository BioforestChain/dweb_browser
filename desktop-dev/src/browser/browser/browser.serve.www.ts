import { Ipc, IpcRequest, IpcResponse } from "../../core/ipc/index.ts";
import { createHttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import type { BrowserNMM } from "./browser.ts";

export async function createWWWServer(this: BrowserNMM) {
  // 为 下载页面做 准备
  const wwwServer = await createHttpDwebServer(this, {
    subdomain: "www",
    port: 433,
  });

  const wwwReadableStreamIpc = await wwwServer.listen();
  wwwReadableStreamIpc.onRequest(onRequest.bind(this));
  return wwwServer;
}

async function onRequest(this: BrowserNMM, request: IpcRequest, ipc: Ipc) {
  let pathname = request.parsed_url.pathname;
  pathname = pathname === "/" ? "/index.html" : pathname;
  const url = `file:///sys/browser/newtab${pathname}?mode=stream`;
  // 打开首页的 路径
  const response = await this.nativeFetch(url);
  ipc.postMessage(
    await IpcResponse.fromResponse(request.req_id, response, ipc)
  );
}
