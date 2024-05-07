import { stringHashCode } from "@dweb-browser/helper/hashCode.ts";
import { IPC_MESSAGE_TYPE, ipcMessageBase } from "../internal/IpcMessage.ts";
import type { OrderBy } from "@dweb-browser/helper/OrderBy.ts";

export type $IpcStreamPulling = ReturnType<typeof IpcStreamPulling>;
/**
 *
 * @param stream_id
 * @param bandwidth 带宽限制, 0 代表不限速。
 * > 负数代表暂停，但对于数据暂停，一般使用 Paused 指令。
 * > 如果出现负数，往往是代表对方的数据处理出现了阻塞，与 Paused 不同，Paused 代表的是逻辑上的暂停，可能是挂起去处理其它事情去了，
 * > 而负数的带宽代表物理意义上的阻塞，此时更不该再发送更多的数据过去
 * @returns
 */
export const IpcStreamPulling = (
  stream_id: string,
  bandwidth?: number | null,
  order: number = stringHashCode(stream_id)
) =>
  ({
    ...ipcMessageBase(IPC_MESSAGE_TYPE.STREAM_PULLING),
    stream_id,
    bandwidth: bandwidth ?? 0,
    order,
  } as const satisfies OrderBy);
