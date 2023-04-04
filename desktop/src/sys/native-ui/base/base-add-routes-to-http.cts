import type { NativeMicroModule } from "../../../core/micro-module.native.cjs";
import type { $BootstrapContext } from "../../../core/bootstrapContext.cjs"
import type { Ipc } from "../../../core/ipc/ipc.cjs";
import type { $IpcMessage } from "../../../core/ipc/const.cjs"
// import type { 
//   $RequestDistributeIpcEventData,
//   $BaseRoute 
// } from "./base-types.cjs"
import { IPC_MESSAGE_TYPE } from "../../../core/ipc/const.cjs"
import { IpcEvent } from "../../../core/ipc/IpcEvent.cjs";
 
/**
 * 提供基础的 HttpConnect 功能的抽象类
 * 1： 创建同 http.sys.dweb 的连接
 * 2： 添加监听的路由
 * 3： 监听同 http.sys.dweb 发送过来的 IpcEvent类型的消息
 * 4： 提供 nativeui 发过来的 wait_for_operation 请求【需要再 继承类中 调用这个方法】
 * 5： 提供 nativeui 发送过来 operation_return 的基础处理 【需要再 继承类中调用这个方法】
 * 6： 提供 把 IpcEvent 发送过来的数据体 转为对象的方法
 * 7： 提供 把 JSON 数据发送给 匹配的 nativeui 的方法
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
    console.log('base-http-connect _httpIpcOnEvent')
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
  _httpIpcOnEventRequestDistributeWaitForOperationBase = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    const app_url = data.url.split("app_url=")[1].split("/index.html")[0]
    this._waitForOperationRequestDistributeIpcEventData.set(app_url, data)
  }

  /**
   * 向 plugin 发送消息
   * @param data 
   * @param httpIpc 
   */
  _httpIpcOnEventRequestDistributeOperationReturnBase = async (data: $RequestDistributeIpcEventData, httpIpc: Ipc) => {
    const id = data.headers.id
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
          body: data.body,
          to: _d.headers.origin // 发送那个 app 对应 virtual-keyboard
        })
      )
    )
    this._reqs.delete(id)
  }

  /**
   * 把 IpcEvent 发送过来的数据 转化为 $RequestDistributeIpcEventData 对象
   * @param data 
   * @returns 
   */
  creageRequestDistributeIpcEventData(data: string | Uint8Array){
    if(Array.isArray(data)) throw new Error(`[${this._nmm.mmid} base-http-connect.cts createStateSendActionItem 非法的参数 data 只能够是JSON字符串]`)
    try{
      const o = JSON.parse(data as string) as $RequestDistributeIpcEventData;
      return o
    }catch(err){
      throw err;
    }
  }

  /**
   * 向 nativeui 发送消息
   * @param body 
   * @param from 
   */
  _postMessageToUI = async (body: string /** JSON.STRING */, from: string) => {
    const route = this._waitForOperationRequestDistributeIpcEventData.get(from)
    if(route === undefined){
      throw new Error(`${this._nmm.mmid} htt-connect _postMessageToUI route === undefined`)
    }
    console.log('_postMesageToUI: body: ', body)
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
  method: "POST" | "GET" | "PUT"
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


