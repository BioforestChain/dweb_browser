import type { $BootstrapContext } from "../../../../core/bootstrapContext.cjs"
import type { Ipc } from "../../../../core/ipc/ipc.cjs";
import type { TorchNMM } from "./torch.cjs"
import { IpcEvent } from "../../../../core/ipc/IpcEvent.cjs";
import { routes } from "./route.cjs"
import url from "node:url"

import { $BaseRoute, BaseAddRoutesToHttp } from "../../base/base-add-routes-to-http.cjs";

// 处理 同 http.sys.dweb 之间的连接
export class AddRoutesToHttp extends BaseAddRoutesToHttp<TorchNMM>{
  /**
   * 手电筒是否打开
   */
  b = false;
  constructor(
    nmm: TorchNMM,
    context:  $BootstrapContext,
  ){
    super(nmm, context, routes as $BaseRoute[])
  }

  _httpIpcOnEventRequestDistribute = async (ipcEvent: IpcEvent, httpIpc: Ipc) => {
    const _d = this.creageRequestDistributeIpcEventData(ipcEvent.data)
    const pathname = url.parse(_d.url).pathname;
    switch(pathname){
      case "/torch.nativeui.sys.dweb/torchState":
        this.httpIpc.postMessage(
          IpcEvent.fromText(
            "http.sys.dweb",
            JSON.stringify({
              action: "state/send",
              pathname: _d.pathname,
              matchMode: _d.matchMode,
              method: _d.method,
              done: true,
              headers: {
                ..._d.headers,
                bodyType: "boolean"
              },
              body: this.b,
              to: _d.headers.origin 
            })
          )
        )
        break;
      case "/torch.nativeui.sys.dweb/toggleTorch":
        this.b = !this.b;
        this.httpIpc.postMessage(
          IpcEvent.fromText(
            "http.sys.dweb",
            JSON.stringify({
              action: "state/send",
              pathname: _d.pathname,
              matchMode: _d.matchMode,
              method: _d.method,
              done: true,
              headers: {
                ..._d.headers,
                bodyType: "boolean"
              },
              body: this.b,
              to: _d.headers.origin 
            })
          )
        )
        break;
      default: throw new Error(`${this._nmm.mmid} http-connect _httpIpcOnEventRequestDistribute 还有没有处理的路由 ${pathname}`)
    }
  }
}