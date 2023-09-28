
import { $JmmAppInstallManifest, FetchEvent, IpcResponse } from "../../../common/deps.ts";
import type { DwebServiceWorkerPlugin } from "./index.ts";

export class ServiceWorkerFetchEvent extends Event {
  constructor(private fetchEvent: FetchEvent,private plugin: DwebServiceWorkerPlugin) {
    super("fetch");
  }
  get request() {
    return this.fetchEvent.request;
  }
  /**查询连接者的信息 */
  async getRemoteManifest():Promise<$JmmAppInstallManifest> {
    const { ipc } = this.fetchEvent;
    const mmid = ipc.remote.mmid;
    const res = await this.plugin.buildApiRequest("/query",{
      pathPrefix:"dns.std.dweb",
      search:{
        mmid:mmid
      }
    }).fetch()
    return await res.json()
  }
  /**
   * 回复消息
   * @param body 
   * @param init 
   */
  async respondWith(body?: BodyInit | null, init?: ResponseInit) {
    const response = new Response(body, init);
    const { ipc, ipcRequest } = this.fetchEvent;
    ipc.postMessage(await IpcResponse.fromResponse(ipcRequest.req_id, response, ipc));
  }
}
