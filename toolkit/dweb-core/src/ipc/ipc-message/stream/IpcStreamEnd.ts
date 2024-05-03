import { IPC_MESSAGE_TYPE, ipcMessageBase } from "../internal/IpcMessage.ts";

export type $IpcStreamEnd = ReturnType<typeof IpcStreamEnd>;
export const IpcStreamEnd = (stream_id: string) =>
  ({ ...ipcMessageBase(IPC_MESSAGE_TYPE.STREAM_END), stream_id } as const);
