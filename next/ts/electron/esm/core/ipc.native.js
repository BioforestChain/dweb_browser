import { MessagePortIpc } from "./ipc-web/MessagePortIpc.js";
export class NativeIpc extends MessagePortIpc {
    constructor(port, remote, role) {
        super(port, remote, role);
    }
}
