// haptics.sys.dweb
import { NativeMicroModule } from "../../../../core/micro-module.native.ts";
import { log } from "../../../../helper/devtools.ts";
import { setHaptics } from "./handlers.ts"
import type { HttpServerNMM } from "../../../http-server/http-server.ts";

export class HapticsNMM extends NativeMicroModule{
  mmid = "haptics.sys.dweb" as const;
  httpNMM: HttpServerNMM | undefined;
  impactLightStyle: $ImpactLightStyle = "HEAVY"
  notificationStyle: $NotificationStyle = "SUCCESS"

  protected _bootstrap() {
    log.green(`[${this.mmid}] _bootstrap`);
    // haptics.sys.dweb/impactLight?X-Dweb-Host=api.browser.sys.dweb%3A443&style=HEAVY
    this.registerCommonIpcOnMessageHandler({
      pathname: "/impactLight",
      matchMode:"full",
      input:{ style: "string" },
      output: "object",
      handler: setHaptics.bind(this)
    })

    // /haptics.sys.dweb/notification?X-Dweb-Host=api.browser.sys.dweb%3A443&style=SUCCESS | WARNING | ERROR
    this.registerCommonIpcOnMessageHandler({
      pathname: "/notification",
      matchMode:"full",
      input:{},
      output: "object",
      handler: setHaptics.bind(this)
    })

    // haptics.sys.dweb/vibrateClick
    this.registerCommonIpcOnMessageHandler({
      pathname: "/vibrateClick",
      matchMode:"full",
      input:{},
      output: "object",
      handler: setHaptics.bind(this)
    })

    // /haptics.sys.dweb/vibrateDisabled
    this.registerCommonIpcOnMessageHandler({
      pathname: "/vibrateDisabled",
      matchMode:"full",
      input:{},
      output: "object",
      handler: setHaptics.bind(this)
    })

    // /haptics.sys.dweb/vibrateDoubleClick
    this.registerCommonIpcOnMessageHandler({
      pathname: "/vibrateDoubleClick",
      matchMode:"full",
      input:{},
      output: "object",
      handler: setHaptics.bind(this)
    })

    // /haptics.sys.dweb/vibrateHeavyClick
    this.registerCommonIpcOnMessageHandler({
      pathname: "/vibrateHeavyClick",
      matchMode:"full",
      input:{},
      output: "object",
      handler: setHaptics.bind(this)
    })
    
    // /haptics.sys.dweb/vibrateTick
    this.registerCommonIpcOnMessageHandler({
      pathname: "/vibrateTick",
      matchMode:"full",
      input:{},
      output: "object",
      handler: setHaptics.bind(this)
    })
    
    // /haptics.sys.dweb/customize?X-Dweb-Host=api.browser.sys.dweb%3A443&duration=300
    this.registerCommonIpcOnMessageHandler({
      pathname: "/customize",
      matchMode:"full",
      input:{},
      output: "object",
      handler: setHaptics.bind(this)
    })
  }

  // private _impactLight = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const url = new URL(req.url as string, `${req.headers.origin}`)
  //   const style =  url.searchParams.get('style') as $ImpactLightStyle;
  //   if(style === null) throw new Error(`${this.mmid} _impactLight style === null`);
  //   this.impactLightStyle = style;
  //   this.sendToUi(JSON.stringify({
  //     operationName: "impactLight", 
  //     value: style,
  //     from: req.headers.origin
  //   }))
  //   res.setHeader('content-type', "text/plain")
  //   res.end(style)
  // } 

  // private _notification = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const url = new URL(req.url as string, `${req.headers.origin}`);
  //   const style = url.searchParams.get('style') as $NotificationStyle;
  //   if(style === null) throw new Error(`${this.mmid} _notification style === null`);
  //   this.notificationStyle = style;
  //   this.sendToUi(JSON.stringify({
  //     operationName: "notification", 
  //     value: style,
  //     from: req.headers.origin
  //   }))
  //   res.setHeader('content-type', "text/plain");
  //   res.end(style)
  // }

  // private _vibrateClick = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   this.sendToUi(JSON.stringify({
  //     operationName: "vibrateClick", 
  //     value: true,
  //     from: req.headers.origin
  //   }))
  //   res.setHeader('content-type', "text/plain");
  //   res.end("vibrateClick")
  // } 

  // private _vibrateDisabled = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   this.sendToUi(JSON.stringify({
  //     operationName: "vibrateDisabled", 
  //     value: true,
  //     from: req.headers.origin
  //   }))
  //   res.setHeader('content-type', "text/plain");
  //   res.end("vibrateDisabled")
  // } 

  // private _vibrateDoubleClick = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   this.sendToUi(JSON.stringify({
  //     operationName: "vibrateDoubleClick", 
  //     value: true,
  //     from: req.headers.origin
  //   }))
  //   res.setHeader('content-type', "text/plain")
  //   res.end("vibrateDoubleClick")
  // }

  // private _vibrateHeavyClick = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   this.sendToUi(JSON.stringify({
  //     operationName: "vibrateHeavyClick", 
  //     value: true,
  //     from: req.headers.origin
  //   }))  
  //   res.setHeader('content-type', "text/plain")
  //   res.end("vibrateHeavyClick")
  // }

  // private _vibrateTick = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   this.sendToUi(JSON.stringify({
  //     operationName: "vibrateTick", 
  //     value: true,
  //     from: req.headers.origin
  //   })) 
  //   res.setHeader('content-type', "text/plain")
  //   res.end("vibrateTick")
  // }

  // private _customize = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const url = new URL(req.url as string, req.headers.origin);
  //   const duration = url.searchParams.get('duration');
  //   if(duration === null) throw new Error(`${this.mmid} _customize duration === null`)
  //   this.duration = parseInt(duration);
  //   this.sendToUi(JSON.stringify({
  //     operationName: "customize", 
  //     value: this.duration,
  //     from: req.headers.origin
  //   })) 
  //   res.setHeader('content-type', "text/plain")
  //   res.end("vibrateTick")
  // }

  // private _waitForOperation = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const url = new URL(req.url as string, req.headers.referer)
  //   const appUrl = url.searchParams.get('app_url')
  //   if(appUrl === null) throw new Error(`${this.mmid} _waitForOperation appUrl === null`)
  //   this.waitForOperationResMap.set(appUrl, res)
  // }

  // private sendToUi = async (data: string) => {
  //   Array.from(this.waitForOperationResMap.values()).forEach(res => {
  //     res.write(new TextEncoder().encode(`${data}\n`))
  //   })
  // }

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }
}

export type $ImpactLightStyle = "HEAVY" | "MEDIUM" | "LIGHT";
export type $NotificationStyle = "SUCCESS" | "WARNING" | "ERROR";
 