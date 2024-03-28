import { IPC_DATA_ENCODING, IPC_MESSAGE_TYPE, IPC_STATE } from "./helper/const.ts";
import { IpcMessage } from "./helper/IpcMessage.ts";

export class IpcLifeCycle extends IpcMessage<IPC_MESSAGE_TYPE.LIFE_CYCLE> {
  constructor(readonly state: IPC_STATE, readonly encoding: IPC_DATA_ENCODING[] = []) {
    super(IPC_MESSAGE_TYPE.LIFE_CYCLE);
  }

  static opening = () => new IpcLifeCycle(IPC_STATE.OPENING);

  static open = () => new IpcLifeCycle(IPC_STATE.OPEN);

  static closing = () => new IpcLifeCycle(IPC_STATE.OPENING);
  static close = () => new IpcLifeCycle(IPC_STATE.CLOSED);
}
