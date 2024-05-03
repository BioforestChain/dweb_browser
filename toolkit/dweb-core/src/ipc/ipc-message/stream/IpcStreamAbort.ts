import { IPC_MESSAGE_TYPE, ipcMessageBase } from "../internal/IpcMessage.ts";

export type $IpcStreamAbort = ReturnType<typeof IpcStreamAbort>;
export const IpcStreamAbort = (stream_id: string) =>
  ({ ...ipcMessageBase(IPC_MESSAGE_TYPE.STREAM_ABORT), stream_id } as const);
