import { stringHashCode } from "@dweb-browser/helper/hashCode.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "../internal/IpcMessage.ts";
import type { OrderBy } from "@dweb-browser/helper/OrderBy.ts";

export type $IpcStreamEnd = ReturnType<typeof IpcStreamEnd>;
export const IpcStreamEnd = (stream_id: string, order: number = stringHashCode(stream_id)) =>
  ({ ...ipcMessageBase(IPC_MESSAGE_TYPE.STREAM_END), stream_id, order } as const satisfies OrderBy);
