import { createHttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import type { BrowserNMM } from "./browser.ts";

export async function createWWWServer(this: BrowserNMM) {
  // 为 下载页面做 准备
  const wwwServer = await createHttpDwebServer(this, {
    subdomain: "www",
    port: 433,
  });

  const API_PREFIX = "/api/";
  const serverIpc = await wwwServer.listen();
  serverIpc.onFetch(async (event) => {
    const { pathname, search } = event.url;
    let url: string;
    if (pathname.startsWith(API_PREFIX)) {
      url = `file://${pathname.slice(API_PREFIX.length)}${search}`;
    } else {
      url = `file:///sys/browser/newtab${pathname}?mode=stream`;
    }
    return await this.nativeFetch(url);
  });
  return wwwServer;
}
