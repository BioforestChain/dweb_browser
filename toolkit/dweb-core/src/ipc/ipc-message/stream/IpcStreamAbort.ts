import type { OrderBy } from "@dweb-browser/helper/OrderBy.ts";
import { stringHashCode } from "@dweb-browser/helper/hashCode.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "../internal/IpcMessage.ts";

export type $IpcStreamAbort = ReturnType<typeof IpcStreamAbort>;
export const IpcStreamAbort = (stream_id: string, order: number = stringHashCode(stream_id)) =>
  ({ ...ipcMessageBase(IPC_MESSAGE_TYPE.STREAM_ABORT), stream_id, order } as const satisfies OrderBy);
