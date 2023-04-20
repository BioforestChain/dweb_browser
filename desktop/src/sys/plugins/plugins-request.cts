import type { $StatusbarPluginsRequestQueueItem } from "./types.cjs"
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs"
import type { IpcResponse } from "../../core/ipc/IpcResponse.cjs"

export class PluginsRequest{
    _allocId: number = 0;
    list = new Map<
        string,
        $StatusbarPluginsRequestQueueItem[]
    >()
    add = async (
      app_url: string, 
      request: IpcRequest, 
      callback: {(id: string): void}
    ) => {
      let statusbarPluginRequest =
            this.list.get(app_url);
      const id = `${this._allocId++}`
      const result = await new Promise<IpcResponse>((resolve) => {
        if (statusbarPluginRequest === undefined) {
          statusbarPluginRequest = [];
          this.list.set(
            app_url,
            statusbarPluginRequest
          );
        }
        statusbarPluginRequest.push({
          body: request.body.raw as ReadableStream<Uint8Array>,
          callback: (reponse: IpcResponse) => {
            resolve(reponse);
          },
          req_id: request.req_id,
          id: id,
        });
        callback(id)
      });
      return result;
    } 
}