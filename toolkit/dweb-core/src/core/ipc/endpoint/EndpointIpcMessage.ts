import type { $IpcMessage, $IpcRawMessage } from "../../index.ts";
import { ENDPOINT_MESSAGE_TYPE } from "./EndpointMessage.ts";
import { endpointMessageBase } from "./internal/EndpointMessage.ts";

export type $EndpointIpcMessage<M extends $IpcMessage = $IpcMessage> = ReturnType<typeof endpointIpcMessage<M>>;
export type $EndpointIpcRawMessage<M extends $IpcRawMessage = $IpcRawMessage> = ReturnType<
  typeof endpointIpcMessage<M>
>;

export const endpointIpcMessage = <M>(pid: number, ipcMessage: M) =>
  ({
    ...endpointMessageBase(ENDPOINT_MESSAGE_TYPE.IPC),
    pid,
    ipcMessage,
  } as const);
