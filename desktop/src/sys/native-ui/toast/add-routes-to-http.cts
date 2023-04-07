import type { $BootstrapContext } from "../../../core/bootstrapContext.cjs"
import type { Ipc } from "../../../core/ipc/ipc.cjs";
import type { ToastNMM } from "./toast.cjs"
import type { IpcEvent } from "../../../core/ipc/IpcEvent.cjs";
import { routes } from "./route.cjs"
import { log } from "../../../helper/devtools.cjs"
import querystring from "node:querystring"
import url from "node:url"

import { $BaseRoute, BaseAddRoutesToHttp, $RequestDistributeIpcEventData } from "../base/base-add-routes-to-http.cjs";

// 处理 同 http.sys.dweb 之间的连接
export class AddRoutesToHttp extends BaseAddRoutesToHttp<ToastNMM>{
  constructor(
    nmm: ToastNMM,
    context:  $BootstrapContext,
  ){
    super(nmm, context, routes as $BaseRoute[])
  }

  _httpIpcOnEventRequestDistribute = async (ipcEvent: IpcEvent, httpIpc: Ipc) => {
    const data = this.creageRequestDistributeIpcEventData(ipcEvent.data)
    const pathname = url.parse(data.url).pathname;
    switch(pathname){
      case "/toast.sys.dweb/show":
        this._httpIpcOnEventRequestDistributeShow(data, httpIpc);
        break;
      case "/toast-ui/wait_for_operation":
        this._httpIpcOnEventRequestDistributeWaitForOperationBase(data, httpIpc);
        break;
      case "/toast-ui/operation_return":
        this._httpIpcOnEventRequestDistributeOperationReturnBase(data, httpIpc);
        break;
      // case "/internal/observe":
      //   this._httpIpcOnEventRequestDistributeInternalObserve(data, httpIpc);
      //   break;
      default: throw new Error(`${this._nmm.mmid} http-connect _httpIpcOnEventRequestDistribute 还有没有处理的路由 ${pathname}`)
    }
  }

  _httpIpcOnEventRequestDistributeShow = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    const id = this._allcId++;
    this._reqs.set(id, data)
    const searchParams = querystring.parse(data.url);
    console.log('---id: ', id)
    this._postMessageToUI(
      JSON.stringify({
        action: "operation",
        operationName: "show",
        value: {
          message: searchParams.message,
          duration: searchParams.duration,
          position: searchParams.position
        },
        from: data.headers.origin,
        id: id
      }),
      data.headers.origin
    )
  }

  // /**
  //  * 监听
  //  * @param data 
  //  * @param httpIpc 
  //  */
  // _httpIpcOnEventRequestDistributeInternalObserve = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
  //   // 如果状态发生改变 可以从这里进行触发
  //   // 需要注意的一般用不上 返回数据的方法 同 getState
  //   // 但是 done === false
  //   const app_url= data.headers.origin;
  //   this._observe.set(app_url, data)
  // }

  // _httpIpcOnEventRequestDistributeStartObserve = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
  //   // 保存起来
  //   // 是否可以通过这个发送一个消息给 查看是否能够实现
  //   const app_url = data.headers.origin
  //   this._startObserve.set(app_url, data)
  //   log.red(`${this._nmm.mmid } _httpIpcOnEventRequestDistributeStartObserve 还没有实际的业务逻辑`)
  // } 

  // _httpIpcOnEventRequestDistributeStopOverve = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
  //   const app_url = data.headers.origin;
  //   this._startObserve.delete(app_url);
  //   log.red(`暂时在测试上用来添加一个 隐藏 vitrual-keyboard 的功能 但是还没有实现`)
  // }
 
  // _httpIpcOnEventRequestDistributeGetState = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
  //   const id = this._allcId++;
  //   this._reqs.set(id, data)
  //   this._postMessageToUI(
  //     {
  //       action: "operation",
  //       operationName: "get_state",
  //       value: "",
  //       from: data.headers.origin,
  //       id: id
  //     },
  //     data.headers.origin
  //   )
  // }

  // _httpIpcOnEventRequestDistributeSetState = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
  //   const id = this._allcId++;
  //   this._reqs.set(id, data)
  //   const _searchParams = querystring.parse(data.url);

  //   throw new Error(`${this._nmm.mmid}_httpIpcOnEventRequestDistributeSetState 还有没处理的 setState 请求`)
  // }

  // _httpIpcOnEventRequestDistributeSetBackgroundColor = (colorJson: string, from: string, id: number, httpIpc: Ipc) => {
  //   const color = JSON.parse(colorJson)
  //   this._postMessageToUI(
  //     {
  //       action: "operation",
  //       operationName: "set_background_color",
  //       value: converRGBAToHexa(color.red, color.green, color.blue, color.alpha),
  //       from: from,
  //       id: id
  //     },
  //     from
  //   )
  // }
}