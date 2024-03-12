import { IpcPool } from "../index.ts";
import type { $MicroModuleManifest } from "../types.ts";
import { MessagePortIpc } from "./MessagePortIpc.ts";

export class NativeIpc extends MessagePortIpc {
  constructor(port: MessagePort, remote: $MicroModuleManifest, override channelId: string, override endpoint: IpcPool) {
    super(port, remote, channelId, endpoint);
  }
}
