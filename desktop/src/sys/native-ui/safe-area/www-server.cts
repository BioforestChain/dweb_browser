import { log } from "../../../helper/devtools.cjs";
import { BaseWWWServer } from "../base_www_server.cjs"
import type { $IpcMessage  } from "../../../core/ipc/const.cjs";
import type { Ipc } from "../../../core/ipc/ipc.cjs"
import type { SafeAreaNMM } from "./safe-area.cjs"

export class WWWServer extends BaseWWWServer<SafeAreaNMM>{
    constructor(
        nmm: SafeAreaNMM,
    ){
        super(nmm)
    }
    _onRequestMore = async (message: $IpcMessage , ipc: Ipc) => {
        log.red(`${this.nmm.mmid} base_www_server.cts _onMessage 还有消息没有处理 ${JSON.stringify(message)}`)
    }
}