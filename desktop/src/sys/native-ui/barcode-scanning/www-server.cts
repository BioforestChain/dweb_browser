import { log } from "../../../helper/devtools.cjs";
import { IPC_MESSAGE_TYPE } from "../../../core/ipc/const.cjs"
import { createHttpDwebServer } from "../../http-server/$createHttpDwebServer.cjs";
import { IpcResponse } from "../../../core/ipc/IpcResponse.cjs";
import { BaseWWWServer } from "../base_www_server.cjs"
import type { HttpDwebServer } from "../../http-server/$createHttpDwebServer.cjs"
import type { $IpcMessage  } from "../../../core/ipc/const.cjs";
import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs";
import type { Ipc } from "../../../core/ipc/ipc.cjs"
import type { BarcodeScanningNativeUiNMM } from "./barcode-scanning.cjs"

export class WWWServer extends BaseWWWServer<BarcodeScanningNativeUiNMM>{
    constructor(
        nmm: BarcodeScanningNativeUiNMM,
    ){
        super(nmm)
    }
    _onRequestMore = async (message: $IpcMessage , ipc: Ipc) => {
        log.red(`${this.nmm.mmid} www-server.cts.cts _onMessage 还有消息没有处理 ${JSON.stringify(message)}`)
    }
}