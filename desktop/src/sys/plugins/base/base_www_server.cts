import { log } from "../../../helper/devtools.cjs";
import { IPC_MESSAGE_TYPE } from "../../../core/ipc/const.cjs"
import { createHttpDwebServer } from "../../http-server/$createHttpDwebServer.cjs";
import { IpcResponse } from "../../../core/ipc/IpcResponse.cjs";
import type { HttpDwebServer } from "../../http-server/$createHttpDwebServer.cjs"
import type { $IpcMessage  } from "../../../core/ipc/const.cjs";
import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs";
import type { Ipc } from "../../../core/ipc/ipc.cjs"
import type { NativeMicroModule } from "../../../core/micro-module.native.cjs";

/**
 * 提供静态资源服务
 */
export abstract class BaseWWWServer<T extends NativeMicroModule>{
  server: HttpDwebServer | undefined;
  constructor(
    readonly nmm: T,
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
      default: this._onRequestMore(message, ipc)
      // default: 
    }
  }

  private _onRequest = async (request: IpcRequest , ipc: Ipc) => {
    let pathname = request.parsed_url.pathname;
    if (pathname === "/" || pathname === "/index.html") {
      pathname = `/html/${this.nmm.mmid}.html`;
    }
    const url = `file:///assets/${pathname}?mode=stream`;
    const remoteIpcResponse = await this.nmm.nativeFetch(url);
    ipc.postMessage(
      await IpcResponse.fromResponse(
      request.req_id,
      remoteIpcResponse,
      ipc
      )
    );
  }
  abstract _onRequestMore(message: $IpcMessage , ipc: Ipc): void;
}