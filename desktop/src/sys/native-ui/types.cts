import type { IpcResponse } from "../../core/ipc/IpcResponse.cjs";

export type $StatusbarPluginsRequestMap 
  = Map<string, $StatusbarPluginsRequestQueueItem[]>

export interface $StatusbarPluginsRequestQueueItem {
  body: ReadableStream<Uint8Array>;
  callback: { (response: IpcResponse): void };
  req_id: number;
  id: string; // 队列项的标识符
}

 