 
// state = {
//     overlay: boolean
//     内部区域
//     cutoutInsets: {
//         left: number;
//         top: number;
//         right: number;
//         bottom: number;
//     }
//     外部区域
//     outerInsets: {
//         left: number;
//         top: number;
//         right: number;
//         bottom: number;
//     }
// }

import { NativeMicroModule } from "../../../../core/micro-module.native.cjs";
import { log } from "../../../../helper/devtools.cjs"
import querystring from "node:querystring"
import { converRGBAToHexa } from "../../helper.cjs";
import { 
  getState, setState, startObserve, stopObserve
 } from "./handlers.cjs"
import { IpcEvent } from "../../../../core/ipc/IpcEvent.cjs";
import { ipcMain, IpcMainEvent } from "electron/main";
import type { HttpServerNMM } from "../../../http-server/http-server.cjs";
import type { $ReqRes, $Observe } from "../status-bar/status-bar.main.cjs";
import type { IncomingMessage, OutgoingMessage } from "http";
import type { Ipc } from "../../../../core/ipc/ipc.cjs";
 

export class SafeAreaNMM extends NativeMicroModule {
  mmid = "safe-area.nativeui.sys.dweb" as const;
  httpIpc: Ipc | undefined
  observes: Map<string /** headers.host */ , Ipc> = new Map();
  observesState: Map<string /**headers.host */, boolean>  = new Map();
  encoder = new TextEncoder();

  // httpNMM: HttpServerNMM | undefined;
  // observe: Map<string, OutgoingMessage> = new Map();
  // waitForOperationRes: Map<string, OutgoingMessage> = new Map();
  // reqResMap: Map<number, $ReqRes> = new Map();
  // observeMap: Map<string, $Observe> = new Map() 
  // encoder = new TextEncoder();
  // allocId = 0;
 
  _bootstrap = async (context: any) => {
    log.green(`[${this.mmid} _bootstrap]`)
    {
      // 监听从 multi-webview-comp-status-bar.html.mts 通过 ipcRenderer 发送过来的 监听数据
      ipcMain.on(
        'safe_area_update', 
        (ipcMainEvent: IpcMainEvent, host, state) => {
          const b = this.observesState.get(host)
          if(b === true){
            const ipc = this.observes.get(host);
            if(ipc === undefined) throw new Error(`ipc === undefined`);
            ipc.postMessage(
              IpcEvent.fromText(
                "observe",
                `${JSON.stringify(state)}`
              )
            )
          }
        }
      )
    }

    this.registerCommonIpcOnMessageHandler({
      pathname: "/getState",
      matchMode: "full",
      input: {},
      output: "object",
      handler: getState.bind(this)
    });    
    
    this.registerCommonIpcOnMessageHandler({
      pathname: "/setState",
      matchMode: "full",
      input: {},
      output: "object",
      handler: setState.bind(this)
    });
     
    this.registerCommonIpcOnMessageHandler({
      pathname: "/startObserve",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: startObserve.bind(this)
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/stopObserve",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: stopObserve.bind(this)
    });
   
  }


  // private _startObserve = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const origin = req.headers.origin;
  //   if(origin === undefined) throw new Error(`origin === null`)
  //   let observe = this.observeMap.get(origin);
  //   if(observe === undefined) {
  //     this.observeMap.set(origin, {isObserve: true, res: undefined});
  //   }else{
  //     observe.isObserve = true;
  //   }
  //   res.end()
  // }

  // /**
  //  * 停止监听
  //  * @param req 
  //  * @param res 
  //  */
  // private _stopObserve = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const origin = req.headers.origin;
  //   if(origin === undefined) throw new Error(`origin === undefined`)
  //   let observe = this.observeMap.get(origin);
  //   if(observe === undefined) throw new Error(`observe === undefined`)
  //   observe.res?.end(); // 释放监听的 res
  //   this.observeMap.delete(origin);
  //   res.end()
  // }

  // private _getState = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const origin = req.headers.origin;
  //   if(origin === undefined) throw new Error(`origin === null`)
  //   const waitForRes = this.waitForOperationRes.get(origin)
  //   if(waitForRes === undefined) throw new Error(`waitForRes ===  undefined`);
  //   const id = this.allocId++;
  //   // 把请求发送给 UI
  //   waitForRes
  //   .write(
  //     this.encoder.encode(
  //       `${JSON.stringify({
  //         operationName: "get_state",
  //         from: origin,
  //         id: id
  //       })}\n`
  //     )
  //   )
  //   // 把请求保存做起来
  //   this.reqResMap.set(id, {req, res})
  // }

  // private _setState = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const origin = req.headers.origin;
  //   if(origin === undefined) throw new Error(`origin === null`);
  //   const waitForRes = this.waitForOperationRes.get(origin)
  //   if(waitForRes === undefined) throw new Error(`waitForRes ===  undefined`);
  //   const searchParams = querystring.parse(req.url as string);
  //   const id = this.allocId++;
  //   // 把请求保存做起来
  //   this.reqResMap.set(id, {req, res})

  //   if(searchParams.overlay !== undefined && typeof searchParams.overlay === "string"){
  //     waitForRes.write(
  //       this.encoder.encode(
  //         `${JSON.stringify({
  //           operationName: "set_overlay",
  //           value: searchParams.overlay === "true" ? true : false,
  //           from: origin,
  //           id: id
  //         })}\n`
  //       )
  //     )
  //     return;
  //   }
  //   throw new Error(`非法的请求 ${req.url}`)
  // }
  
  // /**
  //  * 添加 ovserve
  //  * @param req 
  //  * @param res 
  //  * @returns 
  //  */
  // private _observe = async (req: IncomingMessage, res: OutgoingMessage) => {
  //   const origin = req.headers.origin;
  //   if(origin === undefined) throw new Error(`origin === null`)
  //   const mmid = querystring.parse(req.url as string).mmid
  //   if(mmid === undefined) throw new Error(`mmid === undefined`);
  //   if(Object.prototype.toString.call(mmid).slice(8, -1) === "Array") throw new Error(`mmid === string[]`)
  //   if(mmid !== this.mmid) return;
  //   let observe = this.observeMap.get(origin);
  //   if(observe === undefined) {
  //     this.observeMap.set(origin, {isObserve: false, res: res});
  //     return;
  //   }
  //   observe.res = res;
  // }
  
  
  _shutdown = async () => {

  }
}
