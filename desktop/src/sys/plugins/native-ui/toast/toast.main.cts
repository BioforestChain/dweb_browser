import { NativeMicroModule } from "../../../../core/micro-module.native.cjs";
import { log } from "../../../../helper/devtools.cjs"
import { WWWServer } from "./www-server.cjs"
import querystring from "node:querystring"
import type { IncomingMessage, OutgoingMessage } from "node:http";
import type { HttpServerNMM } from "../../../http-server/http-server.cjs";
import type { $ReqRes, $Observe } from "../status-bar/status-bar.main.cjs";
import type { Ipc } from "../../../../core/ipc/ipc.cjs";

export class ToastNMM extends NativeMicroModule {
  mmid = "toast.nativeui.sys.dweb" as const;
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
      this.httpNMM.addRoute(`/toast.sys.dweb/show`, this._show);
      this.httpNMM.addRoute(`/toast-ui/wait_for_operation`, this._waitForOperation)
      this.httpNMM.addRoute(`/toast-ui/operation_return`, this._operationReturn)
    }
    {
      new WWWServer(this)
    }

  }

  private _show = async (req: IncomingMessage, res: OutgoingMessage) => {
    const origin = req.headers.origin;
    if(origin === undefined) throw new Error(`origin === null`);
    const waitForRes = this.waitForOperationRes.get(origin)
    console.log('this.waitForOperationRes: ', this.waitForOperationRes)
    console.log('orign: ', origin)
    if(waitForRes === undefined) throw new Error(`waitForRes ===  undefined`);
    const searchParams = querystring.parse(req.url as string);
    const id = this.allocId++;
    // 把请求保存做起来
    this.reqResMap.set(id, {req, res})
    waitForRes.write(
      this.encoder.encode(
        `${JSON.stringify({
          operationName: "show",
          value: {
            message: searchParams.message,
            duration: searchParams.duration,
            position: searchParams.position
          },
          from: origin,
          id: id
        })}\n`
      )
    )
  }

  private _waitForOperation = async (req: IncomingMessage, res: OutgoingMessage) => {
    const appUrl = new URL(req.url as string, req.headers.referer).searchParams.get('app_url')
    if(appUrl === null) throw new Error(`${this.mmid} _waiForOperation appUrl === null`)
    this.waitForOperationRes.set(appUrl, res)
  }
  
  private _operationReturn = async (req: IncomingMessage, res: OutgoingMessage) => {
    const id = req.headers.id
    if(typeof id !== "string") throw new Error(`${this.mmid} typeof id !== string`)
    if(Object.prototype.toString.call(id).slice(8, -1) === "Array") throw new Error(`id === Array`)
    let chunks = Buffer.alloc(0);
    req.on('data', (chunk) => {
      chunks = Buffer.concat([chunks, chunk])
    })
    req.on('end', () => {
      res.end()
      const key = parseInt(id)
      const reqRes = this.reqResMap.get(key)
      if(reqRes === undefined) throw new Error(`reqRes === undefined`);
      reqRes.res.end(Buffer.from(chunks));
      this.reqResMap.delete(key);
    })
   
  }

  _shutdown = async () => {

  }
}