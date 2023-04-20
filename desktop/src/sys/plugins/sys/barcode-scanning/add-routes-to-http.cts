import type { $BootstrapContext } from "../../../../core/bootstrapContext.cjs"
import type { Ipc } from "../../../../core/ipc/ipc.cjs";
import type { BarcodeScanningNativeUiNMM } from "./barcode-scanning.cjs"
import type { $RequestDistributeIpcEventData } from "../../base/base-add-routes-to-http.cjs"
import { routes } from "./route.cjs"
import { IpcEvent } from "../../../../core/ipc/IpcEvent.cjs";
import url from "node:url"
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
    // http://localhost:22605/barcode-scanning.sys.dweb/process?X-Dweb-Host=api.browser.sys.dweb%3A443&rotation=0&formats=QR_CODE
    switch(pathname){
      // case "/status-bar.nativeui.sys.dweb/getState":
      //   this._httpIpcOnEventRequestDistributeGetState(data, httpIpc);
      //   break;
      // case "/status-bar.nativeui.sys.dweb/setState":
      //   this._httpIpcOnEventRequestDistributeSetState(data, httpIpc);
      //   break;
      // case "/status-bar.nativeui.sys.dweb/startObserve":
      //   this._httpIpcOnEventRequestDistributeStartObserve(data, httpIpc);
      //   break;
      // case "/status-bar.nativeui.sys.dweb/stopObserve":
      //   this._httpIpcOnEventRequestDistributeStopObserve(data, httpIpc);
      //   break;
      case "/barcode-scanning.sys.dweb/process":
          this._httpIpcOnEventRequestDistributeProcess(data, httpIpc);
          break;
      case "/barcode-scanning-ui/wait_for_operation":
        this._httpIpcOnEventRequestDistributeWaitForOperationBase(data, httpIpc);
        break;
      // case "/barcode-scanning-ui/operation_return":
      //   this._httpIpcOnEventRequestDistributeOperationReturnBase(data, httpIpc)
      //   // this._httpIpcOnEventRequestDistributeOperationReturn(data, httpIpc);
      //   break;
      // case "/internal/observe":
      //   this._httpIpcOnEventRequestDistributeInternalObserve(data, httpIpc);
      //   break;
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
     
      const arr = Uint8Array.from(Object.values(data.body))
      const buffer = Buffer.from(arr)
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


  // /**
  //  * 设置状态
  //  * @param data 
  //  * @param httpIpc 
  //  * @returns 
  //  */
  // _httpIpcOnEventRequestDistributeSetState = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
  //   const id = this._allcId++;
  //   this._reqs.set(id, data)
  //   const _searchParams = querystring.parse(data.url);
    
  //   if(_searchParams.color !== undefined && typeof _searchParams.color === "string") {
  //     const color = JSON.parse(_searchParams.color)
  //     return this._postMessageToUI(
  //       JSON.stringify({
  //         action: "operation",
  //         operationName: "set_background_color",
  //         value: converRGBAToHexa(color.red, color.green, color.blue, color.alpha),
  //         from: data.headers.origin,
  //         id: id
  //       }),
  //       data.headers.origin
  //     )
  //   }
    
  //   if(_searchParams.style !== undefined && typeof _searchParams.style === "string"){
  //     return  this._postMessageToUI(
  //       JSON.stringify({
  //         action: "operation",
  //         operationName: "set_style",
  //         value: _searchParams.style,
  //         from: data.headers.origin,
  //         id: id
  //       }),
  //       data.headers.origin
  //     )
  //   }

  //   if(_searchParams.overlay !== undefined && typeof _searchParams.overlay === "string"){
  //     return this._postMessageToUI(
  //       JSON.stringify({
  //         action: "operation",
  //         operationName: "set_overlay",
  //         value: _searchParams.overlay === "false" ? false : true,
  //         from: data.headers.origin,
  //         id: id
  //       }),
  //       data.headers.origin
  //     )
  //   }

  //   if(_searchParams.visible !== undefined && typeof _searchParams.visible === "string"){
  //     return  this._postMessageToUI(
  //       JSON.stringify({
  //         action: "operation",
  //         operationName: "set_visible",
  //         value: _searchParams.visible === 'false' ? false : true,
  //         from: data.headers.origin,
  //         id: id
  //       }),
  //       data.headers.origin
  //     )
  //   }

  //   throw new Error(`${this._nmm.mmid} _httpIpcOnEventRequestDistributeSetState 还有没处理的 setState 请求`)
  // }

  // /**
  //  * 设置背景颜色
  //  * @param colorJson 
  //  * @param from 
  //  * @param id 
  //  * @param httpIpc 
  //  */
  // _httpIpcOnEventRequestDistributeSetBackgroundColor = (colorJson: string, from: string, id: number, httpIpc: Ipc) => {
  //   const color = JSON.parse(colorJson)
  //   this._postMessageToUI(
  //     JSON.stringify({
  //       action: "operation",
  //       operationName: "set_background_color",
  //       value: converRGBAToHexa(color.red, color.green, color.blue, color.alpha),
  //       from: from,
  //       id: id
  //     }),
  //     from
  //   )
  // }


  // /**
  //  * 处理 nativeui 操作完成后返回的消息向 plugin 发送消息
  //  * @param data 
  //  * @param httpIpc 
  //  */
  // _httpIpcOnEventRequestDistributeOperationReturn = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
  //   this._httpIpcOnEventRequestDistributeOperationReturnBase(
  //     data, 
  //     '/status-bar.nativeui.sys.dweb/getState',
  //     httpIpc
  //   )/** 返回数据给 发起请求的操作 */
  // }
 
// }