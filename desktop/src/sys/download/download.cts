// 实现下载的模块

import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { log } from "../../helper/devtools.cjs";
import querystring from "node:querystring";
import { WWWServer } from "./www-server.cjs";
import type { IncomingMessage, OutgoingMessage } from "http";
import type { $BootstrapContext } from "../../core/bootstrapContext.cjs";
import type { HttpServerNMM } from "../http-server/http-server.cjs";

export class DownloadNMM extends NativeMicroModule{
  mmid = "download.sys.dweb" as const;
  httpNMM: HttpServerNMM | undefined;
  waitForOperationResMap: Map<string, OutgoingMessage> = new Map()

  protected async _bootstrap(context: $BootstrapContext) {
    log.green(`[${this.mmid}] _bootstrap`);

    this.httpNMM = (await context.dns.query('http.sys.dweb')) as HttpServerNMM
    if(this.httpNMM === undefined) throw new Error(`[${this.mmid}] this.httpNMM === undefined`)

    {
      new WWWServer(this)
    }
  }

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }
}

 