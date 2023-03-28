import path from "node:path"
import fsPromises from "node:fs/promises";
import { log } from "../../../helper/devtools.cjs";
import { IPC_MESSAGE_TYPE } from "../../../core/ipc/const.cjs"
import { createHttpDwebServer } from "../../http-server/$createHttpDwebServer.cjs";
import { IpcHeaders } from "../../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../../core/ipc/IpcResponse.cjs";
import { reqadHtmlFile } from "../read-file.cjs"
import type { HttpDwebServer } from "../../http-server/$createHttpDwebServer.cjs"
import type { $IpcMessage  } from "../../../core/ipc/const.cjs";
import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs";
import type { Ipc } from "../../../core/ipc/ipc.cjs"
import type { StatusbarNativeUiNMM } from "./status-bar.main.cjs"
 
export class WWWServer{
    server: HttpDwebServer | undefined;
    constructor(
        readonly nmm: StatusbarNativeUiNMM,
    ){
       this._int()
    }

    private _int = async () => {
        this.server = await createHttpDwebServer(this.nmm, {});
        log.green(`[${this.nmm.mmid}] ${this.server.startResult.urlInfo.internal_origin}`);
        (await this.server.listen()).onMessage(this._onMessage)
    }

    private _onMessage = (message: $IpcMessage , ipc: Ipc) => {
        switch(message.type){
            case IPC_MESSAGE_TYPE.REQUEST: 
                this._onRequest(message, ipc);
                break;
        }
    }

    private _onRequest = (request: IpcRequest , ipc: Ipc) => {
        const pathname = request.parsed_url.pathname;
        switch(pathname){
            case "/" || "/index.html":
                this._onRequestIndex(request, ipc);
                break;
            default: throw new Error(`${this.nmm.mmid} 还有没有处理器的 www-server request ${request.url}`,)
        }
    }

    private _onRequestIndex = async (request: IpcRequest , ipc: Ipc) => {
        ipc.postMessage(
            await IpcResponse.fromText(
                request.req_id,
                200,
                new IpcHeaders({
                "Content-type": "text/html",
                }),
                await reqadHtmlFile('status-bar'),
                ipc
            )
        );
        return this;
    }
}