// haptics.sys.dweb
import type { IncomingMessage, OutgoingMessage } from "http";
import type { $BootstrapContext } from "../../../../core/bootstrapContext.cjs";
import { IpcResponse } from "../../../../core/ipc/IpcResponse.cjs";
import type { MicroModule } from "../../../../core/micro-module.cjs";
import { NativeMicroModule } from "../../../../core/micro-module.native.cjs";
import { log } from "../../../../helper/devtools.cjs";
import { WWWServer } from "./www-server.cjs"
import type { HttpServerNMM } from "../../../http-server/http-server.cjs";

export class HapticsNMM extends NativeMicroModule{
  mmid = "haptics.sys.dweb" as const;
  httpNMM: HttpServerNMM | undefined;
  impactLightStyle: $ImpactLightStyle = "HEAVY"
  notificationStyle: $NotificationStyle = "SUCCESS"
  duration: number = 0;
  waitForOperationResMap: Map<string, OutgoingMessage> = new Map()

  protected async _bootstrap(context: $BootstrapContext) {
    log.green(`[${this.mmid}] _bootstrap`);

    this.httpNMM = (await context.dns.query('http.sys.dweb')) as HttpServerNMM
    if(this.httpNMM === undefined) throw new Error(`[${this.mmid}] this.httpNMM === undefined`)

    // haptics.sys.dweb/impactLight?X-Dweb-Host=api.browser.sys.dweb%3A443&style=HEAVY
    this.httpNMM.addRoute(`/${this.mmid}/impactLight`, this._impactLight)
    // /haptics.sys.dweb/notification?X-Dweb-Host=api.browser.sys.dweb%3A443&style=SUCCESS
    this.httpNMM.addRoute(`/${this.mmid}/notification`, this._notification)
    // /haptics.sys.dweb/vibrateClick
    this.httpNMM.addRoute(`/${this.mmid}/vibrateClick`, this._vibrateClick)
    // vibrateDisabled
    this.httpNMM.addRoute(`/${this.mmid}/vibrateDisabled`, this._vibrateDisabled)
    // vibrateDoubleClick
    this.httpNMM.addRoute(`/${this.mmid}/vibrateDoubleClick`, this._vibrateDoubleClick)
    // vibrateHeavyClick
    this.httpNMM.addRoute(`/${this.mmid}/vibrateHeavyClick`, this._vibrateHeavyClick)
    // vibrateTick
    this.httpNMM.addRoute(`/${this.mmid}/vibrateTick`, this._vibrateTick)
    // /haptics.sys.dweb/customize?X-Dweb-Host=api.browser.sys.dweb%3A443&duration=300
    this.httpNMM.addRoute(`/${this.mmid}/customize`, this._customize)
    // /haptics.sys.dweb/wait_for_operation
    this.httpNMM.addRoute(`/haptics_sys_dweb_ui/wait_for_operation`, this._waitForOperation)

    // 静态服务器
    {
      new WWWServer(this)
    }
  }

  private _impactLight = async (req: IncomingMessage, res: OutgoingMessage) => {
    const url = new URL(req.url as string, `${req.headers.origin}`)
    const style =  url.searchParams.get('style') as $ImpactLightStyle;
    if(style === null) throw new Error(`${this.mmid} _impactLight style === null`);
    this.impactLightStyle = style;
    this.sendToUi(JSON.stringify({
      operationName: "impactLight", 
      value: style,
      from: req.headers.origin
    }))
    res.setHeader('content-type', "text/plain")
    res.end(style)
  } 

  private _notification = async (req: IncomingMessage, res: OutgoingMessage) => {
    const url = new URL(req.url as string, `${req.headers.origin}`);
    const style = url.searchParams.get('style') as $NotificationStyle;
    if(style === null) throw new Error(`${this.mmid} _notification style === null`);
    this.notificationStyle = style;
    this.sendToUi(JSON.stringify({
      operationName: "notification", 
      value: style,
      from: req.headers.origin
    }))
    res.setHeader('content-type', "text/plain");
    res.end(style)
  }

  private _vibrateClick = async (req: IncomingMessage, res: OutgoingMessage) => {
    this.sendToUi(JSON.stringify({
      operationName: "vibrateClick", 
      value: true,
      from: req.headers.origin
    }))
    res.setHeader('content-type', "text/plain");
    res.end("vibrateClick")
  } 

  private _vibrateDisabled = async (req: IncomingMessage, res: OutgoingMessage) => {
    this.sendToUi(JSON.stringify({
      operationName: "vibrateDisabled", 
      value: true,
      from: req.headers.origin
    }))
    res.setHeader('content-type', "text/plain");
    res.end("vibrateDisabled")
  } 

  private _vibrateDoubleClick = async (req: IncomingMessage, res: OutgoingMessage) => {
    this.sendToUi(JSON.stringify({
      operationName: "vibrateDoubleClick", 
      value: true,
      from: req.headers.origin
    }))
    res.setHeader('content-type', "text/plain")
    res.end("vibrateDoubleClick")
  }

  private _vibrateHeavyClick = async (req: IncomingMessage, res: OutgoingMessage) => {
    this.sendToUi(JSON.stringify({
      operationName: "vibrateHeavyClick", 
      value: true,
      from: req.headers.origin
    }))  
    res.setHeader('content-type', "text/plain")
    res.end("vibrateHeavyClick")
  }

  private _vibrateTick = async (req: IncomingMessage, res: OutgoingMessage) => {
    this.sendToUi(JSON.stringify({
      operationName: "vibrateTick", 
      value: true,
      from: req.headers.origin
    })) 
    res.setHeader('content-type', "text/plain")
    res.end("vibrateTick")
  }

  private _customize = async (req: IncomingMessage, res: OutgoingMessage) => {
    const url = new URL(req.url as string, req.headers.origin);
    const duration = url.searchParams.get('duration');
    if(duration === null) throw new Error(`${this.mmid} _customize duration === null`)
    this.duration = parseInt(duration);
    this.sendToUi(JSON.stringify({
      operationName: "customize", 
      value: this.duration,
      from: req.headers.origin
    })) 
    res.setHeader('content-type', "text/plain")
    res.end("vibrateTick")
  }

  private _waitForOperation = async (req: IncomingMessage, res: OutgoingMessage) => {
    const url = new URL(req.url as string, req.headers.referer)
    const appUrl = url.searchParams.get('app_url')
    if(appUrl === null) throw new Error(`${this.mmid} _waitForOperation appUrl === null`)
    this.waitForOperationResMap.set(appUrl, res)
  }

  private sendToUi = async (data: string) => {
    Array.from(this.waitForOperationResMap.values()).forEach(res => {
      res.write(new TextEncoder().encode(`${data}\n`))
    })
  }

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }
}

export type $ImpactLightStyle = "HEAVY" | "MEDIUM" | "LIGHT";
export type $NotificationStyle = "SUCCESS" | "WARNING" | "ERROR";
 