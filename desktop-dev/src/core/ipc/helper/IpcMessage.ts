import { $IpcMessage, IPC_MESSAGE_TYPE } from "./const.ts";

/**基础ipc消息*/
export class IpcMessage<T extends IPC_MESSAGE_TYPE> {
  constructor(readonly type: T) {}
}

/**分发消息到各个ipc的监听时使用*/
export class IpcPoolPack {
  constructor(readonly pid: number, readonly ipcMessage: $IpcMessage) {}
}

export class IpcPoolPackString {
  constructor(readonly pid: number, readonly ipcMessage: string) {}
}

/**消息传递时包裹pool消息📦*/
export class PackIpcMessage {
  constructor(readonly pid: number, readonly messageByteArray: Uint8Array) {}
}
