// 模拟状态栏模块-用来提供状态UI的模块
import { NativeMicroModule } from "../../../../core/micro-module.native.cjs";
import { WWWServer } from "./www-server.cjs"
import { log } from "../../../../helper/devtools.cjs"
import { converRGBAToHexa } from "../../helper.cjs";
import querystring from "node:querystring"
import type { IncomingMessage, OutgoingMessage } from "http";
import type { Ipc } from "../../../../core/ipc/ipc.cjs";
import type { HttpServerNMM } from "../../../http-server/http-server.cjs";
import type { $ReqRes, $Observe } from "../status-bar/status-bar.main.cjs";

export class NavigationBarNMM extends NativeMicroModule {
  mmid = "navigation-bar.nativeui.sys.dweb" as const;
  httpIpc: Ipc | undefined
  httpNMM: HttpServerNMM | undefined;
  observe: Map<string, OutgoingMessage> = new Map();
  waitForOperationRes: Map<string, OutgoingMessage> = new Map();
  reqResMap: Map<number, $ReqRes> = new Map();
  observeMap: Map<string, $Observe> = new Map() 
  encoder = new TextEncoder();
  allocId = 0;

  _bootstrap = async (context: any) => {
    log.green(`[${this.mmid} _bootstrap]`)

    this.httpNMM = (await context.dns.query('http.sys.dweb')) as HttpServerNMM
    if(this.httpNMM === undefined) throw new Error(`[${this.mmid}] this.httpNMM === undefined`)
     
    {
      this.httpNMM.addRoute(`/${this.mmid}/startObserve`, this._startObserve)
      this.httpNMM.addRoute(`/${this.mmid}/stopObserve`, this._stopObserve);
      this.httpNMM.addRoute(`/${this.mmid}/getState`, this._getState);
      this.httpNMM.addRoute(`/${this.mmid}/setState`, this._setState);
      this.httpNMM.addRoute(`/internal/observe`, this._observe);
      this.httpNMM.addRoute(`/navigation-bar-ui/wait_for_operation`, this._waitForOperation)
      this.httpNMM.addRoute(`/navigation-bar-ui/operation_return`, this._operationReturn)
    }
    
    {
      new WWWServer(this)
    }

  }

  private _startObserve = async (req: IncomingMessage, res: OutgoingMessage) => {
    const origin = req.headers.origin;
    if(origin === undefined) throw new Error(`origin === null`)
    let observe = this.observeMap.get(origin);
    if(observe === undefined) {
      this.observeMap.set(origin, {isObserve: true, res: undefined});
    }else{
      observe.isObserve = true;
    }
    res.end()
  }

  /**
   * 停止监听
   * @param req 
   * @param res 
   */
  private _stopObserve = async (req: IncomingMessage, res: OutgoingMessage) => {
    const origin = req.headers.origin;
    if(origin === undefined) throw new Error(`origin === undefined`)
    let observe = this.observeMap.get(origin);
    if(observe === undefined) throw new Error(`observe === undefined`)
    observe.res?.end(); // 释放监听的 res
    this.observeMap.delete(origin);
    res.end()
  }

  private _getState = async (req: IncomingMessage, res: OutgoingMessage) => {
    const origin = req.headers.origin;
    if(origin === undefined) throw new Error(`origin === null`)
    const waitForRes = this.waitForOperationRes.get(origin)
    if(waitForRes === undefined) throw new Error(`waitForRes ===  undefined`);
    const id = this.allocId++;
    // 把请求发送给 UI
    waitForRes
    .write(
      this.encoder.encode(
        `${JSON.stringify({
          operationName: "get_state",
          from: origin,
          id: id
        })}\n`
      )
    )
    // 把请求保存做起来
    this.reqResMap.set(id, {req, res})
  }

  private _setState = async (req: IncomingMessage, res: OutgoingMessage) => {
    const origin = req.headers.origin;
    if(origin === undefined) throw new Error(`origin === null`);
    const waitForRes = this.waitForOperationRes.get(origin)
    if(waitForRes === undefined) throw new Error(`waitForRes ===  undefined`);
    const searchParams = querystring.parse(req.url as string);
    const id = this.allocId++;
    // 把请求保存做起来
    this.reqResMap.set(id, {req, res})
    if(searchParams.color !== undefined && typeof searchParams.color === "string"){
      const color = JSON.parse(searchParams.color)
      waitForRes.write(
        this.encoder.encode(
          `${JSON.stringify({
            operationName: "set_background_color",
            value: converRGBAToHexa(color.red, color.green, color.blue, color.alpha),
            from: origin,
            id: id
          })}\n`
        )
      )
      return;
    }

    if(searchParams.style !== undefined && typeof searchParams.style === "string"){
      waitForRes.write(
        this.encoder.encode(
          `${JSON.stringify({
            operationName: "set_style",
            value: searchParams.style,
            from: origin,
            id: id
          })}\n`
        )
      )
      return;
    }

    if(searchParams.overlay !== undefined && typeof searchParams.overlay === "string"){
      waitForRes.write(
        this.encoder.encode(
          `${JSON.stringify({
            operationName: "set_overlay",
            value: searchParams.overlay === "true" ? true : false,
            from: origin,
            id: id
          })}\n`
        )
      )
      return;
    }

    if(searchParams.visible !== undefined && typeof searchParams.visible === "string"){
      waitForRes.write(
        this.encoder.encode(
          `${JSON.stringify({
            operationName: "set_visible",
            value: searchParams.visible === "true" ? true : false,
            from: origin,
            id: id
          })}\n`
        )
      )
      return;
    }

    throw new Error(`非法的请求 ${req.url}`)
  }
  
  /**
   * 添加 ovserve
   * @param req 
   * @param res 
   * @returns 
   */
  private _observe = async (req: IncomingMessage, res: OutgoingMessage) => {
    const origin = req.headers.origin;
    if(origin === undefined) throw new Error(`origin === null`)
    const mmid = querystring.parse(req.url as string).mmid
    if(mmid === undefined) throw new Error(`mmid === undefined`);
    if(Object.prototype.toString.call(mmid).slice(8, -1) === "Array") throw new Error(`mmid === string[]`)
    if(mmid !== this.mmid) return;
    let observe = this.observeMap.get(origin);
    if(observe === undefined) {
      this.observeMap.set(origin, {isObserve: false, res: res});
      return;
    }
    observe.res = res;
  }
  
  private _waitForOperation = async (req: IncomingMessage, res: OutgoingMessage) => {
    const origin = req.headers.origin;
    const appUrl = new URL(req.url as string, req.headers.referer).searchParams.get('app_url')
    if(appUrl === null) throw new Error(`${this.mmid} _waiForOperation appUrl === null`)
    this.waitForOperationRes.set(appUrl, res)
  }
  
  private _operationReturn = async (req: IncomingMessage, res: OutgoingMessage) => {
    const id = req.headers.id
    if(id === undefined) throw new Error(`id === undefined`)
    if(Object.prototype.toString.call(id).slice(8, -1) === "Array") throw new Error(`id === Array`)
    let chunks = Buffer.alloc(0);
    req.on('data', (chunk) => {
      chunks = Buffer.concat([chunks, chunk])
    })
    req.on('end', () => {
      res.end()
      const reqRes = this.reqResMap.get(parseInt(id as string))
      if(id !== "observe"){ // 如果不是监听需要按正常路径返回
        if(reqRes === undefined) throw new Error(`reqRes === undefined`);
        reqRes.res.end(Buffer.from(chunks));
      } 

      // 是否需要 按监听的请求返回数据
      // 如果是 getState 请求出发的 直接返回
      if(id !== "observe" && reqRes !== undefined && reqRes?.req.url?.includes("getState")) return;

      // 如果 是监听触发的 || 是非 getState 的请求触发的需要 需要通过监听路径返回
      const origin = req.headers.from
      if(origin === undefined) throw new Error(`origin === undefined`);
      const observe = this.observeMap.get(origin);
      if(id !== "observe" && observe === undefined) throw new Error(`observe === undefined`);
      // 需要加入一个 \n 符号
      observe?.res?.write(Buffer.concat([chunks, this.encoder.encode("\n")]))
    })
   
  }

  _shutdown = async () => {

  }

}


