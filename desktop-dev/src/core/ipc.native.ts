import type { $MicroModule } from "./helper/types.ts";
import { MessagePortIpc } from "./ipc-web/MessagePortIpc.ts";
import type { IPC_ROLE } from "./ipc/index.ts";

export class NativeIpc extends MessagePortIpc {
  constructor(port: MessagePort, remote: $MicroModule, role: IPC_ROLE) {
    super(port, remote, role);
  }
}
