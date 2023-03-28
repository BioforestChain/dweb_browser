import type { $BootstrapContext } from "../../../core/bootstrapContext.cjs"
import type { Ipc } from "../../../core/ipc/ipc.cjs";
import type { $IpcMessage } from "../../../core/ipc/const.cjs"
import type { StatusbarNativeUiNMM } from "./status-bar.main.cjs"
import { IPC_MESSAGE_TYPE } from "../../../core/ipc/const.cjs"
import { routes } from "./http-route.cjs"
import { IpcEvent } from "../../../core/ipc/IpcEvent.cjs";
import { converRGBAToHexa } from "../helper.cjs"
import { log } from "../../../helper/devtools.cjs"
import querystring from "node:querystring"
import url from "node:url"

// 处理 同 http.sys.dweb 之间的连接
export class HttpConnect{
  private _ipc: Ipc | undefined;
  private _allcId: number = 0;
  private _waitForOperationRequestDistributeIpcEventData: Map<string, $RequestDistributeIpcEventData> = new Map()
  private _reqs = new Map<number, $RequestDistributeIpcEventData>()
  private _startObserve = new Map<string,$RequestDistributeIpcEventData>() 
  private _observe = new Map<string, $RequestDistributeIpcEventData>()
  constructor(
      private readonly _nmm: StatusbarNativeUiNMM,
      private readonly _context:  $BootstrapContext,
      private readonly _mmid: $MMID
  ){
    this._init();
  }

  _init = async () => {
    const [httpIpc] = await this._context.dns.connect('http.sys.dweb')
    httpIpc.onMessage(this._httpIpcOnMessage)
    // 向 httpIpc 发起初始化消息
    // intercept(httpIpc, this._mmid)
    // 添加路由
    routes.forEach(route => httpIpc.postMessage(
      IpcEvent.fromText(
        "http.sys.dweb",
        JSON.stringify({
          ...route,
          action: "routes/add"
        })
      )
    ))
    this._ipc = httpIpc
  }

  _httpIpcOnMessage = async (ipcMessage: $IpcMessage, httpIpc: Ipc) => {
      switch(ipcMessage.type){
          case IPC_MESSAGE_TYPE.EVENT:
              this._httpIpcOnEvent(ipcMessage, httpIpc);
              break;
          default: throw new Error(`status-bar.nativeui.sys.dweb http-ipc-on-message 还有没有处理的 类型`)
      }
  }

  _httpIpcOnEvent = async (ipcMessage: IpcEvent, httpIpc: Ipc) => {
    // const data = 
    switch(ipcMessage.name){
        case "request/distribute":
            this._httpIpcOnEventRequestDistribute(ipcMessage, httpIpc);
            break;
        default: throw new Error(`[status-bar.nativeui.sys.dweb htp-ipc-on-event] 还没没处理的 message ${ipcMessage.name}`);
    }
  } 

  _httpIpcOnEventRequestDistribute = async (ipcEvent: IpcEvent, httpIpc: Ipc) => {
    const data = creageRequestDistributeIpcEventData(ipcEvent.data)
    const pathname = url.parse(data.url).pathname;
    switch(pathname){
      case "/status-bar.nativeui.sys.dweb/getState":
        this._httpIpcOnEventRequestDistributeGetState(data, httpIpc);
        break;
      case "/status-bar.nativeui.sys.dweb/setState":
        this._httpIpcOnEventRequestDistributeSetState(data, httpIpc);
        break;
      case "/status-bar.nativeui.sys.dweb/startObserve":
        this._httpIpcOnEventRequestDistributeStartObserve(data, httpIpc);
        break;
      case "/status-bar-ui/wait_for_operation":
        this._httpIpcOnEventRequestDistributeWaitForOperation(data, httpIpc);
        break;
      case "/status-bar-ui/operation_return":
        this._httpIpcOnEventRequestDistributeWaitForOperationReturn(data, httpIpc);
        break;
      case "/internal/observe":
        this._httpIpcOnEventRequestDistributeInternalObserve(data, httpIpc);
        break;
      default: throw new Error(`status-bar.nativeui.sys.dweb http-connect _httpIpcOnEventRequestDistribute 还有没有处理的路由`)
    }
  }

  /**
   * UI 等待 操作消息的路由
   * @param data 
   * @param httpIpc 
   */
  _httpIpcOnEventRequestDistributeWaitForOperation = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    const app_url = data.url.split("app_url=")[1].split("/index.html")[0]
    this._waitForOperationRequestDistributeIpcEventData.set(app_url, data)
     
  }

  /**
   * 向 plugin 发送消息
   * @param data 
   * @param httpIpc 
   */
  _httpIpcOnEventRequestDistributeWaitForOperationReturn = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    // 如何发送给请求的
    const id = data.headers.id
    if(id === undefined) throw new Error(`status-bar.nativeui.sys.dweb _httpIpcOnEventRequestDistributeWaitForOperationReturn id === undefined`)
    const _d = this._reqs.get(parseInt(id))
    if(_d === undefined) throw new Error(`status-bar.nativeui.sys.dweb _httpIpcOnEventRequestDistributeWaitForOperationReturn d === undefined`)
    const app_url =  data.url.split("app_url=")[1]
    httpIpc.postMessage(
      IpcEvent.fromText(
        "http.sys.dweb",
        JSON.stringify({
          action: "state/send",
          pathname: _d.pathname,
          matchMode: _d.matchMode,
          method: _d.method,
          done: true,
          body: data.body,
          to: app_url // 发送那个 app 对应 statusbar
        })
      )
    )
    this._reqs.delete(id)
  }

  /**
   * 监听
   * @param data 
   * @param httpIpc 
   */
  _httpIpcOnEventRequestDistributeInternalObserve = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    // 如果状态发生改变 可以从这里进行触发
    // 需要注意的一般用不上 返回数据的方法 同 getState
    // 但是 done === false
    const app_url= data.headers.origin;
    this._observe.set(app_url, data)
  }

  _httpIpcOnEventRequestDistributeStartObserve = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    // 保存起来
    const app_url = data.headers.origin
    this._startObserve.set(app_url, data)
  } 
 
  _httpIpcOnEventRequestDistributeGetState = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    const id = this._allcId++;
    this._reqs.set(id, data)
    this._postMessageToUI(
      {
        action: "operation",
        operationName: "get_state",
        value: "",
        from: data.headers.origin,
        id: id
      },
      httpIpc,
      data.headers.origin
    )
  }

  _httpIpcOnEventRequestDistributeSetState = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    const id = this._allcId++;
    this._reqs.set(id, data)
    const _searchParams = querystring.parse(data.url);
    
    if(_searchParams.color !== undefined && typeof _searchParams.color === "string") {
      return this._httpIpcOnEventRequestDistributeSetBackgroundColor(
        _searchParams.color,
        data.headers.origin,
        id,
        httpIpc
      )
    }

    if(_searchParams.style !== undefined && typeof _searchParams.style === "string"){
      return  this._postMessageToUI(
        {
          action: "operation",
          operationName: "set_style",
          value: _searchParams.style,
          from: data.headers.origin,
          id: id
        },
        httpIpc,
        data.headers.origin
      )
    }

    if(_searchParams.overlay !== undefined && typeof _searchParams.overlay === "string"){
      return this._postMessageToUI(
        {
          action: "operation",
          operationName: "set_overlay",
          value: _searchParams.overlay === "false" ? false : true,
          from: data.headers.origin,
          id: id
        },
        httpIpc,
        data.headers.origin
      )
    }

    if(_searchParams.visible !== undefined && typeof _searchParams.visible === "string"){
      return  this._postMessageToUI(
        {
          action: "operation",
          operationName: "set_visible",
          value: _searchParams.visible === 'false' ? false : true,
          from: data.headers.origin,
          id: id
        },
        httpIpc,
        data.headers.origin
      )
    }

    throw new Error(`status-bar.nativeui.sys.dweb _httpIpcOnEventRequestDistributeSetState 还有没处理的 setState 请求`)
  }

  _httpIpcOnEventRequestDistributeSetBackgroundColor = (colorJson: string, from: string, id: number, httpIpc: Ipc) => {
    const color = JSON.parse(colorJson)
    this._postMessageToUI(
      {
        action: "operation",
        operationName: "set_background_color",
        value: converRGBAToHexa(color.red, color.green, color.blue, color.alpha),
        from: from,
        id: id
      },
      httpIpc,
      from
    )
  }

  _postMessageToUI = async (body:Object, httpIpc: Ipc, from: string) => {
    const route = this._waitForOperationRequestDistributeIpcEventData.get(from)
    if(route === undefined){
      throw new Error(`status-bar.nativeui.sys.dweb htt-connect _postMessageToUI route === undefined`)
    }
     // 把请求发送给 UI
    httpIpc.postMessage(
      IpcEvent.fromText(
        "http.sys.dweb",
        JSON.stringify({
          action: "state/send",
          pathname: route.pathname,
          matchMode: route.matchMode,
          method: route.method,
          done: false,
          body: body
        })
      )
    )
  }
  
}



function creageRequestDistributeIpcEventData(data: string | Uint8Array){
  if(Array.isArray(data)) throw new Error('[http-sever.cts createStateSendActionItem 非法的参数 data 只能够是JSON字符串]')
  try{
    const o = JSON.parse(data as string) as $RequestDistributeIpcEventData;
    return o
  }catch(err){
    throw err;
  }
}

export interface $RequestDistributeIpcEventData{
  referer: string;
  pathname: string;
  method: string;
  url: string;
  headers: any;
  matchMode: "full" | "prefix";
  body: any
}