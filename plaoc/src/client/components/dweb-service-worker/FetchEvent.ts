import { FetchEvent, IpcResponse } from "../../../common/deps.ts";


export type $FetchEventType = "fetch";

export class ServiceWorkerFetchEvent extends Event {
  constructor(private fetchEvent: FetchEvent) {
    super("fetch");
  }
  get request() {
    return this.fetchEvent.request;
  }
  async respondWith(body?: BodyInit | null, init?: ResponseInit) {
    const response = new Response(body, init);
    const { ipc, ipcRequest } = this.fetchEvent;
    ipc.postMessage(await IpcResponse.fromResponse(ipcRequest.req_id, response, ipc));
  }
}
