import { $CommonAppManifest } from "../../types.ts";
import { IPC_MESSAGE_TYPE } from "../helper/const.ts";
import { IpcMessage } from "./IpcMessage.ts";

/**
 * 这里会告知 fork的发起者 是否是自动启动，以及自启动的原因
 * 接受者可以用来参考，但无需遵循一致，唯一需要一致的只有 pid
 */
export class IpcFork extends IpcMessage<IPC_MESSAGE_TYPE.FORK> {
  constructor(
    readonly pid: number,
    readonly autoStart: boolean,
    readonly locale: $CommonAppManifest,
    readonly remote: $CommonAppManifest,
    readonly startReason?: string
  ) {
    super(IPC_MESSAGE_TYPE.FORK);
  }
}
