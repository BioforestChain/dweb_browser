export const enum IPC_DATA_TYPE {
   REQUEST,
   RESPONSE,
}
export class IpcRequest {
   readonly type = IPC_DATA_TYPE.REQUEST;
   constructor(
      readonly req_id: number,
      readonly method: string,
      readonly url: string,
      readonly body: string,
      readonly headers: Record<string, string>
   ) {}
   #parsed_url?: URL;
   get parsed_url() {
      return (this.#parsed_url ??= new URL(this.url));
   }
}
export class IpcResponse {
   readonly type = IPC_DATA_TYPE.RESPONSE;
   constructor(
      readonly req_id: number,
      readonly statusCode: number,
      readonly body: string,
      readonly headers: Record<string, string>
   ) {}
}

let ipc_uid_acc = 0;
export abstract class Ipc {
   readonly uid = ipc_uid_acc++;
   abstract postMessage(data: IpcRequest | IpcResponse): void;
   abstract onMessage(
      cb: (message: IpcRequest | IpcResponse) => unknown
   ): () => boolean;
   abstract close(): void;
   abstract onClose(cb: () => unknown): () => boolean;
}
