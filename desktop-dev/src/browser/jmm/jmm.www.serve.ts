import { Ipc, IpcRequest, IpcResponse } from "../../core/ipc/index.ts";
import { createHttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import type { JmmNMM } from "./jmm.ts";

export async function createWWWServer(this: JmmNMM) {
  // 为 下载页面做 准备
  this.wwwServer = await createHttpDwebServer(this, {
    subdomain: "www",
    port: 6363,
  });

  const wwwReadableStreamIpc = await this.wwwServer.listen();
  wwwReadableStreamIpc.onRequest(onRequest.bind(this));
}

async function onRequest(this: JmmNMM, request: IpcRequest, ipc: Ipc) {
  let pathname = request.parsed_url.pathname;
  pathname = pathname === "/" ? "/index.html" : pathname;
  const url = `file:///sys/browser/jmm${pathname}?mode=stream`;
  // 打开首页的 路径
  const response = await this.nativeFetch(url);
  ipc.postMessage(
    await IpcResponse.fromResponse(request.req_id, response, ipc)
  );
}
