import { log } from "../../../../helper/devtools.cjs";
import { BaseWWWServer } from "../../base/base_www_server.cjs"
import type { $IpcMessage  } from "../../../../core/ipc/const.cjs";
import type { Ipc } from "../../../../core/ipc/ipc.cjs"
import type { NavigationBarNMM } from "./navigation-bar.main.cjs"

export class WWWServer extends BaseWWWServer<NavigationBarNMM>{
    constructor(
        nmm: NavigationBarNMM,
    ){
        super(nmm)
    }
    _onRequestMore = async (message: $IpcMessage , ipc: Ipc) => {
        log.red(`${this.nmm.mmid} base_www_server.cts _onMessage 还有消息没有处理 ${JSON.stringify(message)}`)
    }
}