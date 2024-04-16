import { $IpcMessage } from "../helper/const.ts";
import { EndpointLifecycle } from "./EndpointLifecycle.ts";

/**消息类型 */
export const enum ENDPOINT_MESSAGE_TYPE {
  LIFECYCLE = 0,
  IPC = 1,
}

export type $EndpointMessage = EndpointIpcMessage | EndpointLifecycle;

/**
 * 总的消息类型抽象
 */
export class EndpointMessage<T extends ENDPOINT_MESSAGE_TYPE = ENDPOINT_MESSAGE_TYPE> {
  constructor(readonly type: T) {}
}

/**分发消息到各个ipc的监听时使用
 *
 * 这里的 orderBy 和 IpcMessage 里头的 orderBy 不冲突，是两层各自去排序
 */

export class EndpointIpcMessage extends EndpointMessage<ENDPOINT_MESSAGE_TYPE.IPC> {
  constructor(readonly pid: number, readonly ipcMessage: $IpcMessage) {
    super(ENDPOINT_MESSAGE_TYPE.IPC);
  }
}
