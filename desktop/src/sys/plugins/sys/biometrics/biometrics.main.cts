import { NativeMicroModule } from "../../../../core/micro-module.native.cjs";
import { log } from "../../../../helper/devtools.cjs";
import querystring from "node:querystring"
import type { HttpServerNMM } from "../../../http-server/http-server.cjs";
import type { IncomingMessage, OutgoingMessage } from "node:http";
import type { $BootstrapContext } from "../../../../core/bootstrapContext.cjs";

export class BiometricsNMM extends NativeMicroModule{
  mmid = "biometrics.sys.dweb" as const;
  allocId = 0;
  httpNMM: HttpServerNMM | undefined;
  impactLightStyle: $ImpactLightStyle = "HEAVY"
  notificationStyle: $NotificationStyle = "SUCCESS"
  duration: number = 0;
  waitForOperationResMap: Map<string, OutgoingMessage> = new Map()
  encoder = new TextEncoder();
  reqResMap: Map<number, $ReqRes> = new Map();

  protected async _bootstrap(context: $BootstrapContext) {
    log.green(`[${this.mmid}] _bootstrap`);
 
    this.registerCommonIpcOnMessageHandler({
      pathname: "/check",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: async () => {
        return true
      }
    })

    this.registerCommonIpcOnMessageHandler({
      pathname: "/biometrics",
      matchMode: "full",
      input: {},
      output: "object",
      handler: async () => {
        return await this.nativeFetch(`file://mwebview.sys.dweb/plubin/biommetrices`);
      }
    })
  }

  // private _check = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const origin = req.headers.origin;
  //   if(origin === undefined) throw new Error(`${this.mmid} origin === undefined`);
    
  //   const waitForRes = this.waitForOperationResMap.get(origin);
  //   if(waitForRes === undefined) {
  //     console.log('origin: ', origin)
  //     console.log('this.waitForOperationResMap: ', this.waitForOperationResMap)
  //     throw new Error(`${this.mmid} waitForRes === undefined`)
  //   };
  //   const id = this.allocId++;
  //   const value = this.encoder.encode(
  //     `${JSON.stringify({
  //       operationName: "check",
  //       value: "",
  //       from: origin,
  //       id: id 
  //     })}\n`
  //   )
  //   waitForRes.write(value)
  //   this.reqResMap.set(id, {req, res});
  // }

  // private _biometrics = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const origin = req.headers.origin;
  //   if(origin === undefined) throw new Error(`origin === undefined`);
    
  //   const waitForRes = this.waitForOperationResMap.get(origin);
  //   if(waitForRes === undefined) throw new Error(`waitForRes === undefined`)

  //   const id = this.allocId++;
  //   const value = this.encoder.encode(`
  //     ${JSON.stringify({
  //       operationName: "biometrics",
  //       value: '',
  //       from: origin,
  //       id: id
  //     })}
  //   `)
  //   waitForRes.write(value)
  //   this.reqResMap.set(id, {req, res});
  // }

  // private _waitForOperation = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const queyString = req.url?.split("?")[1]
  //   if(queyString === undefined) throw new Error(`${this.mmid} search === undefined`);
  //   const parsedUrlQuery = querystring.parse(queyString);
  //   const appUrl = parsedUrlQuery.app_url;
  //   if(appUrl === undefined) throw new Error(`${this.mmid} appUrl === undefined`);
  //   if(typeof appUrl === "string"){
  //     return this.waitForOperationResMap.set(appUrl, res);
  //   }
  //   throw new Error(`${this.mmid} appUrl === Array`)
  // }

  // private _operationReturn = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   let chunks = Buffer.alloc(0);
  //   req.on('data', (chunk) => {
  //     chunks = Buffer.concat([chunks, chunk]);
  //   })
  //   req.on('end', () => {
  //     res.end();
  //     const {err, id} = this.getIdFromReq(req);
  //     if(err) throw err;
  //     const key = parseInt(id)
  //     const reqRes = this.reqResMap.get(key);
  //     if(reqRes === undefined){
  //       console.log('id', id)
  //       console.log("this.reqResMap: ", this.reqResMap)
  //       throw new Error(`reqRes === undefined`)
  //     };
      
  //     reqRes.res.end(chunks);
  //     this.reqResMap.delete(key)
  //   })
  // }

  private getIdFromReq = (req: IncomingMessage) => {
    const id = req.headers.id;
    if(id === undefined){
      return {
        err: new Error(`id === undefined`),
        id: "",
      }
    }

    if(typeof id === "string"){
      return {
        err: null,
        id: id
      }
    }

    return {
      err: new Error(`id === Array`),
      id: ""
    }
  }

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }
}

export type $ImpactLightStyle = "HEAVY" | "MEDIUM" | "LIGHT";
export type $NotificationStyle = "SUCCESS" | "WARNING" | "ERROR";
export interface $ReqRes{
  req: IncomingMessage, 
  res: OutgoingMessage
}
 