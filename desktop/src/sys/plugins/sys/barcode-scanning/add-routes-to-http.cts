import type { $BootstrapContext } from "../../../../core/bootstrapContext.cjs"
import type { Ipc } from "../../../../core/ipc/ipc.cjs";
import type { BarcodeScanningNativeUiNMM } from "./barcode-scanning.cjs"
import type { $RequestDistributeIpcEventData } from "../../base/base-add-routes-to-http.cjs"
import { routes } from "./route.cjs"
import { IpcEvent } from "../../../../core/ipc/IpcEvent.cjs";
import url from "node:url"
import {log} from "../../../../helper/devtools.cjs"
import { BaseAddRoutesToHttp } from "../../base/base-add-routes-to-http.cjs"

/**
 * 向 http.sys.dweb 注册路由的类
 */
export class AddRoutesToHttp extends BaseAddRoutesToHttp<BarcodeScanningNativeUiNMM>{

  constructor(
    nmm: BarcodeScanningNativeUiNMM,
    context:  $BootstrapContext,
  ){
    super(nmm, context, routes)
  }

  _httpIpcOnEventRequestDistribute = async (ipcEvent: IpcEvent, httpIpc: Ipc) => {
    const data = this.creageRequestDistributeIpcEventData(ipcEvent.data)
    const pathname = url.parse(data.url).pathname;
    switch(pathname){
      case "/barcode-scanning.sys.dweb/process":
          this._httpIpcOnEventRequestDistributeProcess(data, httpIpc);
          break;
      default: throw new Error(`${this._nmm.mmid} http-connect _httpIpcOnEventRequestDistribute 还有没有处理的路由 ${pathname}`)
    }
  }

  private _httpIpcOnEventRequestDistributeProcess = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    const pathname = url.parse(data.url).pathname;
    switch(data.method){
      case "OPTIONS":
        this._httpIpcOnEventRequestDistributeProcessOPTIONS(data, httpIpc)
        break;
      case "POST":
        this._httpIpcOnEventRequestDistributeProcessPOST(data, httpIpc)
        break;
      default: throw new Error(`${this._nmm.mmid} http-connect _httpIpcOnEventRequestDistributeProcess 还有没有处理的路由 ${pathname}`)
    }
  }

  private _httpIpcOnEventRequestDistributeProcessPOST = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    this._parseQRCODE(data, httpIpc)
  }

  private _httpIpcOnEventRequestDistributeProcessOPTIONS = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    this.httpIpc.postMessage(
      IpcEvent.fromText(
        "http.sys.dweb",
        JSON.stringify({
          action: "state/send",
          pathname: data.pathname,
          matchMode: data.matchMode,
          method: data.method,
          done: true,
          headers:{
            bodyType: "string"
          },
          body: "",
          to: data.headers.origin // 发送那个 app 对应 virtual-keyboard
        })
      )
    )
  }

  /**
   * 解析二维码
   * @param data 
   * @param httpIpc 
   */
  private _parseQRCODE = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
      const buffer = Buffer.from(data.body)
      const Jimp = require("jimp");
      const jsQR = require("jsqr");
      Jimp.read(buffer).then(({bitmap}: any) => {
        const result = jsQR(bitmap.data, bitmap.width, bitmap.height);
        this.httpIpc.postMessage(
          IpcEvent.fromText(
            "http.sys.dweb",
            JSON.stringify({
              action: "state/send",
              pathname: data.pathname,
              matchMode: data.matchMode,
              method: data.method,
              done: true,
              headers:{
                bodyType: "string"
              },
              body: JSON.stringify(result === null ? []: [result.data]),
              to: data.headers.origin // 发送那个 app 对应 virtual-keyboard
            })
          )
        )
      })
    };
  }
