import { createHttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";
import type { JmmNMM } from "./jmm.ts";

export async function createWWWServer(this: JmmNMM) {
  // 为 下载页面做 准备
  this.wwwServer = await createHttpDwebServer(this, {
    subdomain: "www",
    port: 6363,
  });

  const serverIpc = await this.wwwServer.listen();
  serverIpc.onFetch(async (event) => {
    let { pathname } = event.url;
    pathname = pathname === "/" ? "/index.html" : pathname;
    const url = `file:///sys/browser/jmm${pathname}?mode=stream`;
    // 打开首页的 路径
    return await this.nativeFetch(url);
  });
}
