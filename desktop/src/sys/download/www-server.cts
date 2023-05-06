import { log } from "../../helper/devtools.cjs";
import { BaseWWWServer } from "../plugins/base/base_www_server.cjs"
import type { $IpcMessage  } from "../../core/ipc/const.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs"
import type { DownloadNMM } from "./download.cjs"

export class WWWServer extends BaseWWWServer<DownloadNMM>{
  constructor(
    nmm: DownloadNMM,
  ){
    super(nmm)
  }
  _onRequestMore = async (message: $IpcMessage , ipc: Ipc) => {
    log.red(`${this.nmm.mmid} www-server.cts.cts _onMessage 还有消息没有处理 ${JSON.stringify(message)}`)
  }
}