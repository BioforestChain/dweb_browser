import type { OutgoingMessage } from "node:http";
import type { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import { log } from "../../helper/devtools.ts";
import type { HttpServerNMM } from "../http-server/http-server.ts";
import { WWWServer } from "./www-server.ts";

// 提供下载的 UI
export class DownloadNMM extends NativeMicroModule {
  mmid = "download.sys.dweb" as const;
  httpNMM: HttpServerNMM | undefined;
  waitForOperationResMap: Map<string, OutgoingMessage> = new Map();

  protected async _bootstrap(context: $BootstrapContext) {
    log.green(`[${this.mmid}] _bootstrap`);

    this.httpNMM = (await context.dns.query("http.sys.dweb")) as HttpServerNMM;
    if (this.httpNMM === undefined)
      throw new Error(`[${this.mmid}] this.httpNMM === undefined`);

    {
      new WWWServer(this);
    }
  }

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }
}
