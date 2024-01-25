import { IPC_DATA_ENCODING, IPC_MESSAGE_TYPE, IPC_STATE, IpcMessage } from "./const.ts";

export class IpcLifeCycle extends IpcMessage<IPC_MESSAGE_TYPE.LIFE_CYCLE> {
  constructor(readonly state: IPC_STATE, readonly encoding: IPC_DATA_ENCODING[] = []) {
    super(IPC_MESSAGE_TYPE.LIFE_CYCLE);
  }
}
