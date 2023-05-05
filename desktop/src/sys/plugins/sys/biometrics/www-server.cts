import { log } from "../../../../helper/devtools.cjs";
import { BaseWWWServer } from "../../base/base_www_server.cjs"
import type { $IpcMessage  } from "../../../../core/ipc/const.cjs";
import type { Ipc } from "../../../../core/ipc/ipc.cjs"
import type { BiometricsNMM } from "./biometrics.main.cjs"

export class WWWServer extends BaseWWWServer<BiometricsNMM>{
  constructor(
    nmm: BiometricsNMM,
  ){
    super(nmm)
  }
  _onRequestMore = async (message: $IpcMessage , ipc: Ipc) => {
    log.red(`${this.nmm.mmid} www-server.cts.cts _onMessage 还有消息没有处理 ${JSON.stringify(message)}`)
  }
}