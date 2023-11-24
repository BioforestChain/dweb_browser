import { buildRequestX } from "../../core/helper/ipcRequestHelper.ts";
import { $MicroModule } from "../../core/types.ts";
import { createHttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";

export async function createWWWServer(this: $MicroModule) {
  // 为 下载页面做 准备
  const wwwServer = await createHttpDwebServer(this, {
    subdomain: "www",
  });

  const API_PREFIX = "/api/";
  const serverIpc = await wwwServer.listen();
  serverIpc.onFetch(async (event) => {
    const { pathname, search } = event.url;
    let url: string;
    if (pathname.startsWith(API_PREFIX)) {
      url = `file://${pathname.slice(API_PREFIX.length)}${search}`;
    } else {
      url = `file:///sys/browser/desk${pathname}?mode=stream`;
    }
    const request = buildRequestX(url, {
      method: event.method,
      headers: event.headers,
      body: event.ipcRequest.body.raw,
    });

    return await this.nativeFetch(request);
  });
  return wwwServer;
}
