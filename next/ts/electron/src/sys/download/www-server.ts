import { log } from "../../helper/devtools.js";
import { BaseWWWServer } from "../plugins/base/base_www_server.js"
import type { $IpcMessage  } from "../../core/ipc/const.js";
import type { Ipc } from "../../core/ipc/ipc.js"
import type { DownloadNMM } from "./download.js"

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