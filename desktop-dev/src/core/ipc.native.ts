import { MessagePortIpc } from "./ipc-web/MessagePortIpc.ts";
import type { IPC_ROLE } from "./ipc/index.ts";
import type { $MicroModule } from "./types.ts";

export class NativeIpc extends MessagePortIpc {
  constructor(port: MessagePort, remote: $MicroModule, role: IPC_ROLE) {
    super(port, remote, role);
  }
}
