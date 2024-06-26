import type { IpcFetchEvent } from "@dweb-browser/core/ipc/helper/ipcFetchHelper.ts";
import { IpcResponse } from "@dweb-browser/core/ipc/index.ts";
import type { $MicroModuleManifest } from "@dweb-browser/core/types.ts";
import type { DwebServiceWorkerPlugin } from "./index.ts";

export class ServiceWorkerFetchEvent extends Event {
  constructor(private fetchEvent: IpcFetchEvent, private plugin: DwebServiceWorkerPlugin) {
    super("fetch");
  }
  get request() {
    return this.fetchEvent.request;
  }
  /**
   * 查询连接者的信息
   * @returns Promise<$MicroModuleManifest>
   */
  async getRemoteManifest(): Promise<$MicroModuleManifest> {
    const { request } = this.fetchEvent;
    // 获取来建立连接的人的mmid
    const mmid = request.headers.get("X-External-Dweb-Host");
    console.log("getRemoteManifest查询id=>", mmid);
    const res = await this.plugin
      .buildApiRequest("/query", {
        pathPrefix: "dns.std.dweb",
        search: {
          mmid: mmid,
        },
      })
      .fetch();
    return await res.json();
  }
  /**
   * 回复消息
   * @param body
   * @param init
   * @returns void
   */
  async respondWith(body?: BodyInit | null, init?: ResponseInit) {
    const response = new Response(body, init);
    const { ipc, ipcRequest } = this.fetchEvent;
    ipc.postMessage(await IpcResponse.fromResponse(ipcRequest.reqId, response, ipc));
  }
}
