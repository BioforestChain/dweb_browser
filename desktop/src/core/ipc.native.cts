import type { $MicroModule } from "../helper/types.cjs";
import { MessagePortIpc } from "./ipc-web/MessagePortIpc.cjs";
import type { IPC_ROLE } from "./ipc/index.cjs";

export class NativeIpc extends MessagePortIpc {
  constructor(port: MessagePort, remote: $MicroModule, role: IPC_ROLE) {
    super(port, remote, role);
  }
}
