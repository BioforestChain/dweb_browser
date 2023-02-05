import { $rawDataToBody } from "./$rawDataToBody.cjs";
import { IPC_DATA_TYPE, type $RawData } from "./const.cjs";
import type { Ipc } from "./ipc.cjs";

export class IpcRequest {
  readonly type = IPC_DATA_TYPE.REQUEST;
  constructor(
    readonly req_id: number,
    readonly method: string,
    readonly url: string,
    readonly rawBody: $RawData,
    readonly headers: Record<string, string>,
    ipc: Ipc
  ) {
    this.#ipc = ipc;
  }
  #ipc: Ipc;
  #body?: ReturnType<typeof $rawDataToBody>;
  get body() {
    return (this.#body ??= $rawDataToBody(this.rawBody, this.#ipc));
  }
  #parsed_url?: URL;
  get parsed_url() {
    return (this.#parsed_url ??= new URL(this.url));
  }
}
