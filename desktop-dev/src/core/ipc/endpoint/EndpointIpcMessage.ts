import { $IpcMessage } from "../helper/const.ts";
import { ENDPOINT_MESSAGE_TYPE } from "./EndpointMessage.ts";
import { endpointMessageBase } from "./internal/EndpointMessage.ts";

export type $EndpointIpcMessage<M extends $IpcMessage> = ReturnType<typeof endpointIpcMessage<M>>;

export const endpointIpcMessage = <M extends $IpcMessage>(pid: number, ipcMessage: M) =>
  ({
    ...endpointMessageBase(ENDPOINT_MESSAGE_TYPE.IPC),
    pid,
    ipcMessage,
  } as const);
