import type { NativeMicroModule } from "../../../core/micro-module.native.cjs";
import type { $BootstrapContext } from "../../../core/bootstrapContext.cjs"
import type { Ipc } from "../../../core/ipc/ipc.cjs";
import type { $IpcMessage } from "../../../core/ipc/const.cjs"
import { IPC_MESSAGE_TYPE } from "../../../core/ipc/const.cjs"
import { IpcEvent } from "../../../core/ipc/IpcEvent.cjs";
import { log } from "../../../helper/devtools.cjs"
import { converRGBAToHexa } from "../helper.cjs"
import querystring from "node:querystring"
import url from "node:url"


 
/**
 * 提供基础的 HttpConnect 功能的抽象类
 * 1： 创建同 http.sys.dweb 的连接
 * 2： 添加监听的路由
 * 3： 监听同 http.sys.dweb 发送过来的 IpcEvent类型的消息
 * 4： 提供 nativeui 发过来的 wait_for_operation 请求【需要再 继承类中 调用这个方法】
 * 5： 提供 nativeui 发送过来 operation_return 的基础处理 【需要再 继承类中调用这个方法】
 * 6： 提供 把 IpcEvent 发送过来的数据体 转为对象的方法
 * 7： 提供 把 JSON 数据发送给 匹配的 nativeui 的方法
 * 8:  提供 处理 ppathname === xxx/getState 的处理方法
 * 9:   
 * 
 * 继承类需要必须要实现
 * 1： _httpIpcOnEventRequestDistribute 方法 用来出来 http.sys.dweb 发送过来的 IpcEvent 类型的消息
 *    1-a: 可以在内部匹配的路由中调用对应的 nativeui 发过来的 wait_for_operation 请求
 *    1-b: 可以在内部匹配的路由中调用对应的 nativeui 发送过来 operation_return 的基础处理
 */
export abstract class BaseAddRoutesToHttp<T extends NativeMicroModule>{
  protected httpIpc: Ipc = undefined as unknown as Ipc;
  protected _waitForOperationRequestDistributeIpcEventData: Map<string, $RequestDistributeIpcEventData> = new Map()
  protected _reqs = new Map<number, $RequestDistributeIpcEventData>()
  protected _observe = new Map<string, $RequestDistributeIpcEventData>()
  protected _encoder = new TextEncoder();
  protected _isObserve: boolean = false;
  protected _allcId: number = 0;
  constructor(
      protected readonly _nmm: T,
      protected readonly _context:  $BootstrapContext,
      protected readonly routes: $BaseRoute[]
  ){
      this._init()
  }

  /**
   * 初始化操作
   */
  private _init = async () => {
    const [httpIpc] = await (this._context.dns as any).privateConnect('http.sys.dweb')
    httpIpc.onMessage(this._httpIpcOnMessage)
    // 添加路由
    this.routes.forEach(route => httpIpc.postMessage(
      IpcEvent.fromText(
        "http.sys.dweb",
        JSON.stringify({
          ...route,
          action: "routes/add"
        })
      )
    ))
    this.httpIpc = httpIpc
  }

  private _httpIpcOnMessage = async (ipcMessage: $IpcMessage, httpIpc: Ipc) => {
    switch(ipcMessage.type){
      case IPC_MESSAGE_TYPE.EVENT:
          this._httpIpcOnEvent(ipcMessage, httpIpc);
          break;
      default: throw new Error(`${this._nmm.mmid} http-ipc-on-message 还有没有处理的 类型`)
    }
  }


  private _httpIpcOnEvent = async (ipcMessage: IpcEvent, httpIpc: Ipc) => {
    switch(ipcMessage.name){
      case "request/distribute":
          this._httpIpcOnEventRequestDistribute(ipcMessage, httpIpc);
          break;
      default: throw new Error(`[${this._nmm.mmid} htp-ipc-on-event] 还没没处理的 message ${ipcMessage.name}`);
    }
  } 

  /**
   * 必须要从写这个方法用来处理具体的请求
   * @param ipcEvent 
   * @param httpIpc 
   */
  abstract _httpIpcOnEventRequestDistribute (ipcEvent: IpcEvent, httpIpc: Ipc): void;
  
  /**
   * UI 等待 操作消息的路由
   * @param data 
   * @param httpIpc 
   */
  protected _httpIpcOnEventRequestDistributeWaitForOperationBase = async (
    data: $RequestDistributeIpcEventData, 
    httpIpc: Ipc,
  ) => {
    const app_url = data.url.split("app_url=")[1].split("/index.html")[0]
    this._waitForOperationRequestDistributeIpcEventData.set(app_url, data)
  }

  /**
   * 向 plugin 发送消息
   * @param data 
   * @param httpIpc 
   */
  protected _httpIpcOnEventRequestDistributeOperationReturnBase = async (
    data: $RequestDistributeIpcEventData, 
    httpIpc: Ipc
  ) => {
    this._httpIpcOnEventRequestDistributeOperationReturnToRequest(data, httpIpc)
    this._httpIpcOnEventRequestDistributeOperationReturnToObserve(data, httpIpc)
  }

  /**
   * 把数据返回给请求
   * @param data 
   * @param httpIpc 
   * @returns 
   */
  protected _httpIpcOnEventRequestDistributeOperationReturnToRequest(data: $RequestDistributeIpcEventData, httpIpc: Ipc){
    const id = data.headers.id
    if(id === 'observe') return /** 不是请求导致的变化 */;
    if(id === undefined) throw new Error(`${this._nmm.mmid} _httpIpcOnEventRequestDistributeWaitForOperationReturn id === undefined`)
    const _d = this._reqs.get(parseInt(id))
    if(_d === undefined) throw new Error(`${this._nmm.mmid} _httpIpcOnEventRequestDistributeWaitForOperationReturn d === undefined`)
    this.httpIpc.postMessage(
      IpcEvent.fromText(
        "http.sys.dweb",
        JSON.stringify({
          action: "state/send",
          pathname: _d.pathname,
          matchMode: _d.matchMode,
          method: _d.method,
          done: true,
          headers:{
            bodyType: "JSON"
          },
          body: new TextDecoder().decode(Buffer.from(data.body)),
          to: _d.headers.origin // 发送那个 app 对应 virtual-keyboard
        })
      )
    )
    this._reqs.delete(id)
  }

  /**
   * 把数据返回给 observe
   * @param data 
   * @param ignorePathname 
   * @param httpIpc 
   * @returns 
   */
  protected _httpIpcOnEventRequestDistributeOperationReturnToObserve(
    data: $RequestDistributeIpcEventData, 
    httpIpc: Ipc
  ){
    if(!this._httpIpcOnEventRequestDistributeOperationReturnToObserveCheck(data)) return;
    const _url = url.parse(data.url.split("app_url=")[1])
    const app_url = `${_url.protocol}//${_url.host}`
    const observe = this._observe.get(app_url)
    if(observe === undefined) {
      log.red(`${this._nmm.mmid} http-connect observe === undefined ${app_url}`)
      return;
    }
     
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
          body: this._encoder.encode(new TextDecoder().decode(Buffer.from(data.body))+"\n"),
          to: app_url // 发送那个 app 对应 statusbar
        })
      )
    )
  }

  /**
   * 检查是否需要返回数据给 Observe
   * 如果 id !== 'observe' || 匹配的请求pathname !== ignorePathname
   * 就需要 把数据返回给 observe
   * @param data 
   * @param ignorePathname 
   * @returns 
   */
  protected _httpIpcOnEventRequestDistributeOperationReturnToObserveCheck(data: $RequestDistributeIpcEventData){
    if(!this._isObserve) return false;
    const id = data.headers.id;
    if(id === "observe") return true;
    const _d = this._reqs.get(parseInt(id))
    if(_d === undefined) throw new Error(`${this._nmm.mmid} _httpIpcOnEventRequestDistributeOperationReturn d === undefined`)
    if(_d.pathname.endsWith('getState')) return false;
    return true;
  }

  /**
   * 把 IpcEvent 发送过来的数据 转化为 $RequestDistributeIpcEventData 对象
   * @param data 
   * @returns 
   */
  protected creageRequestDistributeIpcEventData(data: string | Uint8Array){
    if(Array.isArray(data)) throw new Error(`[${this._nmm.mmid} base-http-connect.cts createStateSendActionItem 非法的参数 data 只能够是JSON字符串]`)
    try{
      const o = JSON.parse(data as string) as $RequestDistributeIpcEventData;
      return o
    }catch(err){
      throw err;
    }
  }

   /**
   * 
   * @param data 开始监听
   * @param httpIpc 
   */
  protected _httpIpcOnEventRequestDistributeStartObserve = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
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
  protected _httpIpcOnEventRequestDistributeStopObserve = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
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
   * 监听 
   * @param data 
   * @param httpIpc 
   */
  protected _httpIpcOnEventRequestDistributeInternalObserve = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    const app_url= data.headers.origin;
    this._observe.set(app_url, data)
    log.red(`接受到了 observe 的请求 app_url ${app_url}`)
  }

  /**
   * 获取状态
   * @param data 
   * @param httpIpc 
   */
  protected _httpIpcOnEventRequestDistributeGetState = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    const id = this._allcId++;
    this._reqs.set(id, data)
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
  protected _httpIpcOnEventRequestDistributeSetState = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    const id = this._allcId++;
    this._reqs.set(id, data)
    const _searchParams = querystring.parse(data.url);
    
    if(_searchParams.color !== undefined && typeof _searchParams.color === "string") {
      const color = JSON.parse(_searchParams.color)
      return this._postMessageToUI(
        JSON.stringify({
          action: "operation",
          operationName: "set_background_color",
          value: converRGBAToHexa(color.red, color.green, color.blue, color.alpha),
          from: data.headers.origin,
          id: id
        }),
        data.headers.origin
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
   * 向 nativeui 发送消息
   * @param body 
   * @param from 
   */
  protected _postMessageToUI = async (body: string /** JSON.STRING */, from: string) => {
    const route = this._waitForOperationRequestDistributeIpcEventData.get(from)
    if(route === undefined){
      console.log(this._waitForOperationRequestDistributeIpcEventData)
      throw new Error(`${this._nmm.mmid} base-add-routes-to-http.cts _postMessageToUI route === undefined from===${from}`)
    }
    // 把请求发送给 UI
    this.httpIpc.postMessage(
      IpcEvent.fromText(
        "http.sys.dweb",
        JSON.stringify({
          action: "state/send",
          pathname: route.pathname,
          matchMode: route.matchMode,
          method: route.method,
          done: false,
          headers:{
            bodyType: "JSON"
          },
          body: body
        })
      )
    )
  }
}

export interface $BaseRoute{
  pathname: string;
  matchMode: "full" | "prefix";
  method: "POST" | "GET" | "PUT" | "OPTIONS"
}

export interface $RequestDistributeIpcEventData{
referer: string;
pathname: string;
method: string;
url: string;
headers: any;
matchMode:  "full" | "prefix";
body: any
}


