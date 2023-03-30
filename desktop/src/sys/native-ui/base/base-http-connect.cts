import type { NativeMicroModule } from "../../../core/micro-module.native.cjs";
import type { $BootstrapContext } from "../../../core/bootstrapContext.cjs"
import type { Ipc } from "../../../core/ipc/ipc.cjs";
import type { $IpcMessage } from "../../../core/ipc/const.cjs"
import { IPC_MESSAGE_TYPE } from "../../../core/ipc/const.cjs"
import { IpcEvent } from "../../../core/ipc/IpcEvent.cjs";
import { converRGBAToHexa } from "../helper.cjs"
import { log } from "../../../helper/devtools.cjs"
import querystring from "node:querystring"
import url from "node:url"
export abstract class BaseHttpConnect<T extends NativeMicroModule>{
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

    _init = async () => {
        // const [httpIpc] = await this._context.dns.connect('http.sys.dweb')
        // console.log(this._context, this._context.connect)
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

    _httpIpcOnMessage = async (ipcMessage: $IpcMessage, httpIpc: Ipc) => {
        switch(ipcMessage.type){
            case IPC_MESSAGE_TYPE.EVENT:
                this._httpIpcOnEvent(ipcMessage, httpIpc);
                break;
            default: throw new Error(`${this._nmm.mmid} http-ipc-on-message 还有没有处理的 类型`)
        }
    }


    _httpIpcOnEvent = async (ipcMessage: IpcEvent, httpIpc: Ipc) => {
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
                body: data.body,
                to: _d.headers.origin // 发送那个 app 对应 virtual-keyboard
                })
            )
        )
        this._reqs.delete(id)
    }

    creageRequestDistributeIpcEventData(data: string | Uint8Array){
        if(Array.isArray(data)) throw new Error('[http-sever.cts createStateSendActionItem 非法的参数 data 只能够是JSON字符串]')
        try{
          const o = JSON.parse(data as string) as $RequestDistributeIpcEventData;
          return o
        }catch(err){
          throw err;
        }
    }

    _postMessageToUI = async (body:Object, from: string) => {
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