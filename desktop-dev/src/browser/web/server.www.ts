import { $MicroModule } from "../../core/types.ts";
import { createHttpDwebServer } from "../../std/http/helper/$createHttpDwebServer.ts";

export async function createWWWServer(this: $MicroModule) {
  // 为 下载页面做 准备
  const wwwServer = await createHttpDwebServer(this, {
    subdomain: "www",
    port: 433,
  });

  const serverIpc = await wwwServer.listen();
  serverIpc.onFetch(async (event) => {
    const { pathname, search } = event.url;
    const url = `file:///sys/browser/web-browser${pathname}?mode=stream`;
    return await this.nativeFetch(url);
  });
  return wwwServer;
}
