import { $MicroModuleManifest } from "../../types.ts";
import { IPC_LIFECYCLE_STATE, IPC_MESSAGE_TYPE } from "../helper/const.ts";
import { IpcMessage } from "./IpcMessage.ts";

export class IpcLifeCycle extends IpcMessage<IPC_MESSAGE_TYPE.LIFE_CYCLE> {
  constructor(
    readonly state: IPC_LIFECYCLE_STATE,
    readonly pid?: number,
    readonly locale?: $MicroModuleManifest,
    readonly remote?: $MicroModuleManifest
  ) {
    super(IPC_MESSAGE_TYPE.LIFE_CYCLE);
  }

  static init(pid: number, locale: $MicroModuleManifest, remote: $MicroModuleManifest) {
    return new IpcLifeCycle(IPC_LIFECYCLE_STATE.INIT, pid, locale, remote);
  }

  static opening = () => new IpcLifeCycle(IPC_LIFECYCLE_STATE.OPENING);

  static open = () => new IpcLifeCycle(IPC_LIFECYCLE_STATE.OPENED);

  static closing = () => new IpcLifeCycle(IPC_LIFECYCLE_STATE.OPENING);
  static close = () => new IpcLifeCycle(IPC_LIFECYCLE_STATE.CLOSED);
}
