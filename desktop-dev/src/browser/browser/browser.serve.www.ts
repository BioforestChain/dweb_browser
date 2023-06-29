import { Ipc, IpcRequest, IpcResponse } from "../../core/ipc/index.ts";
import { createHttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
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
  const { pathname, search } = request.parsed_url;
  // pathname = pathname === "/" ? "/index.html" : pathname;
  let url: string;
  if (pathname.startsWith("/api/")) {
    url = `file://${pathname.slice(5)}${search}`;
  } else {
    url = `file:///sys/browser/newtab${pathname}?mode=stream`;
  }
  const response = await this.nativeFetch(url);
  ipc.postMessage(
    await IpcResponse.fromResponse(request.req_id, response, ipc)
  );
}
