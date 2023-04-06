import type { $BootstrapContext } from "../../../core/bootstrapContext.cjs"
import type { Ipc } from "../../../core/ipc/ipc.cjs";
import type { NavigationBarNMM } from "./navigation-bar.cjs"
import type { $RequestDistributeIpcEventData } from "../base/base-add-routes-to-http.cjs"
import { routes } from "./route.cjs"
import { IpcEvent } from "../../../core/ipc/IpcEvent.cjs";
import { converRGBAToHexa } from "../helper.cjs"
import { log } from "../../../helper/devtools.cjs"
import querystring from "node:querystring"
import url from "node:url"
import { BaseAddRoutesToHttp } from "../base/base-add-routes-to-http.cjs"

/**
 * 向 http.sys.dweb 注册路由的类
 */
export class AddRoutesToHttp extends BaseAddRoutesToHttp<NavigationBarNMM>{
  private _allcId: number = 0;
  private _observe = new Map<string, $RequestDistributeIpcEventData>()
  private _isObserve: boolean = false;
  constructor(
    nmm: NavigationBarNMM,
    context:  $BootstrapContext,
  ){
    super(nmm, context, routes)
  }

  _httpIpcOnEventRequestDistribute = async (ipcEvent: IpcEvent, httpIpc: Ipc) => {
    const data = this.creageRequestDistributeIpcEventData(ipcEvent.data)
    const pathname = url.parse(data.url).pathname;
    switch(pathname){
      case "/navigation-bar.nativeui.sys.dweb/getState":
        this._httpIpcOnEventRequestDistributeGetState(data, httpIpc);
        break;
      case "/navigation-bar.nativeui.sys.dweb/setState":
        this._httpIpcOnEventRequestDistributeSetState(data, httpIpc);
        break;
      case "/navigation-bar.nativeui.sys.dweb/startObserve":
        this._httpIpcOnEventRequestDistributeStartObserve(data, httpIpc);
        break;
      case "/navigation-bar.nativeui.sys.dweb/stopObserve":
        this._httpIpcOnEventRequestDistributeStopObserve(data, httpIpc);
        break;
        case "/navigation-bar-ui/wait_for_operation":
        this._httpIpcOnEventRequestDistributeWaitForOperationBase(data, httpIpc);
        break;
      case "/navigation-bar-ui/operation_return":
        this._httpIpcOnEventRequestDistributeOperationReturn(data, httpIpc);
        break;
      case "/internal/observe":
        this._httpIpcOnEventRequestDistributeInternalObserve(data, httpIpc);
        break;
      default: throw new Error(`${this._nmm.mmid} http-connect _httpIpcOnEventRequestDistribute 还有没有处理的路由 ${pathname}`)
    }
  }

  /**
   * 获取状态
   * @param data 
   * @param httpIpc 
   */
  _httpIpcOnEventRequestDistributeGetState = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    const id = this._allcId++;
    this._reqs.set(id, data)
    const a = JSON.stringify({})
    this._postMessageToUI(
      JSON.stringify({
        action: "operation",
        operationName: "get_state",
        value: "",
        from: data.headers.origin,
        id: id
      }),
      data.headers.origin
    )
  }

  /**
   * 设置状态
   * @param data 
   * @param httpIpc 
   * @returns 
   */
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
        JSON.stringify({
          action: "operation",
          operationName: "set_style",
          value: _searchParams.style,
          from: data.headers.origin,
          id: id
        }),
        data.headers.origin
      )
    }

    if(_searchParams.overlay !== undefined && typeof _searchParams.overlay === "string"){
      return this._postMessageToUI(
        JSON.stringify({
          action: "operation",
          operationName: "set_overlay",
          value: _searchParams.overlay === "false" ? false : true,
          from: data.headers.origin,
          id: id
        }),
        data.headers.origin
      )
    }

    if(_searchParams.visible !== undefined && typeof _searchParams.visible === "string"){
      return  this._postMessageToUI(
        JSON.stringify({
          action: "operation",
          operationName: "set_visible",
          value: _searchParams.visible === 'false' ? false : true,
          from: data.headers.origin,
          id: id
        }),
        data.headers.origin
      )
    }

    throw new Error(`${this._nmm.mmid} _httpIpcOnEventRequestDistributeSetState 还有没处理的 setState 请求`)
  }

  /**
   * 设置背景颜色
   * @param colorJson 
   * @param from 
   * @param id 
   * @param httpIpc 
   */
  _httpIpcOnEventRequestDistributeSetBackgroundColor = (colorJson: string, from: string, id: number, httpIpc: Ipc) => {
    const color = JSON.parse(colorJson)
    this._postMessageToUI(
      JSON.stringify({
        action: "operation",
        operationName: "set_background_color",
        value: converRGBAToHexa(color.red, color.green, color.blue, color.alpha),
        from: from,
        id: id
      }),
      from
    )
  }

  /**
   * 监听 
   * @param data 
   * @param httpIpc 
   */
  _httpIpcOnEventRequestDistributeInternalObserve = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    const app_url= data.headers.origin;
    this._observe.set(app_url, data)
  }

  /**
   * 
   * @param data 开始监听
   * @param httpIpc 
   */
  _httpIpcOnEventRequestDistributeStartObserve = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    // 保存起来
    const app_url = data.headers.origin
    httpIpc.postMessage(
      IpcEvent.fromText(
        'http.sys.dweb',
        JSON.stringify({
          action: "state/send",
          pathname: data.pathname,
          matchMode: data.matchMode,
          method: data.method,
          done: true,
          headers:{
            bodyType: "string"
          },
          body:"",
          to: app_url
        })
      )
    )
    this._isObserve = true;
  } 

  /**
   * 停止监听
   * @param data 
   * @param httpIpc 
   */
  _httpIpcOnEventRequestDistributeStopObserve = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    const app_url = data.headers.origin
    httpIpc.postMessage(
      IpcEvent.fromText(
        'http.sys.dweb',
        JSON.stringify({
          action: "state/send",
          pathname: data.pathname,
          matchMode: data.matchMode,
          method: data.method,
          done: true,
          headers:{
            bodyType: "string"
          },
          body:"",
          to: app_url
        })
      )
    )
    this._isObserve = false;
    this._observe.delete(app_url)
  }


  /**
   * 处理 nativeui 操作完成后返回的消息向 plugin 发送消息
   * @param data 
   * @param httpIpc 
   */
  _httpIpcOnEventRequestDistributeOperationReturn = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    this._httpIpcOnEventRequestDistributeOperationReturnBase(data, httpIpc)/** 返回数据给 发起请求的操作 */
    const id = data.headers.id
    if(id === undefined) throw new Error(`status-bar.nativeui.sys.dweb _httpIpcOnEventRequestDistributeOperationReturn id === undefined`)
    const _d = this._reqs.get(parseInt(id))
    if(_d === undefined) throw new Error(`status-bar.nativeui.sys.dweb _httpIpcOnEventRequestDistributeOperationReturn d === undefined`)
    // const app_url =  data.url.split("app_url=")[1]
    const _url = url.parse(data.url.split("app_url=")[1])
    const app_url = `${_url.protocol}//${_url.host}`
    if(
      !this._isObserve /** 是否还在监听中 */
      || _d.pathname.endsWith('getState') /** 操作的路由不能够是 getState */ 
    ) return;
    const observe = this._observe.get(app_url)
    if(observe === undefined) {
      log.red(`${this._nmm.mmid} http-connect observe === undefined ${app_url}`)
      return;
    }
    const encode = new TextEncoder().encode(data.body+"\n")
    httpIpc.postMessage(
      IpcEvent.fromText(
        "http.sys.dweb",
        JSON.stringify({
          action: "state/send",
          pathname: observe.pathname,
          matchMode: observe.matchMode,
          method: observe.method,
          done: false,
          headers: {
            bodyType: "Uint8Array"
          },
          body: encode,
          to: app_url // 发送那个 app 对应 statusbar
        })
      )
    )
  }
 
}