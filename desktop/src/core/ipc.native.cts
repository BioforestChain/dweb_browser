import type { $MicroModule } from "../helper/types.cjs";
import { MessagePortIpc } from "./ipc-web/MessagePortIpc.cjs";
import type { IPC_ROLE } from "./ipc/index.cjs";

export class NativeIpc extends MessagePortIpc {
  constructor(
    port: MessagePort,
    remote: $MicroModule,
    role: IPC_ROLE,
    /// 原生之间的互相传输，默认支持 message-pack 格式
    support_message_pack = true
  ) {
    super(port, remote, role, support_message_pack);
  }
}
