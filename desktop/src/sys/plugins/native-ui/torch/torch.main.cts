import { NativeMicroModule } from "../../../../core/micro-module.native.cjs";
import type { Ipc } from "../../../../core/ipc/ipc.cjs";
import { log } from "../../../../helper/devtools.cjs"
import type { IncomingMessage, OutgoingMessage } from "http";
import type { HttpServerNMM } from "../../../http-server/http-server.cjs";
import type { $ReqRes, $Observe } from "../status-bar/status-bar.main.cjs";

// @ts-ignore
export class TorchNMM extends NativeMicroModule {
  mmid = "torch.nativeui.sys.dweb" as const;
  httpIpc: Ipc | undefined
  httpNMM: HttpServerNMM | undefined;
  observe: Map<string, OutgoingMessage> = new Map();
  waitForOperationRes: Map<string, OutgoingMessage> = new Map();
  reqResMap: Map<number, $ReqRes> = new Map();
  observeMap: Map<string, $Observe> = new Map() 
  allocId = 0;
  isOpen = false;
  encode = new TextEncoder().encode

  _bootstrap = async (context: any) => {
    log.green(`[${this.mmid} _bootstrap]`)

    // this.httpNMM = (await context.dns.query('http.sys.dweb')) as HttpServerNMM
    // if(this.httpNMM === undefined) throw new Error(`[${this.mmid}] this.httpNMM === undefined`)
    
    // {
    //   this.httpNMM.addRoute(`/${this.mmid}/torchState`, this._torchState);
    //   this.httpNMM.addRoute(`/${this.mmid}/toggleTorch`, this._toggleTorch);
    // }
  }

  private _torchState = async (req: IncomingMessage, res: OutgoingMessage) => {
    res.end(`${this.isOpen}`);
  }

  private _toggleTorch = async (req: IncomingMessage, res: OutgoingMessage) => {
    this.isOpen = !this.isOpen;
    res.end(`${this.isOpen}`);
  }

  _shutdown = async () => {

  }
}