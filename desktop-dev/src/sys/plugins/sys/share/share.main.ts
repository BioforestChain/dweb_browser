import { NativeMicroModule } from "../../../../core/micro-module.native.ts";
import { log } from "../../../../helper/devtools.ts";
import type { HttpServerNMM } from "../../../http-server/http-server.ts";
import { shareShare } from "../../../multi-webview/multi-webview.mobile.handler.ts";

export class ShareNMM extends NativeMicroModule {
  mmid = "share.sys.dweb" as const;
  httpNMM: HttpServerNMM | undefined;

  protected _bootstrap() {
    log.green(`[${this.mmid}] _bootstrap`);

    this.registerCommonIpcOnMessageHandler({
      method: "POST",
      pathname: "/share",
      matchMode: "full",
      input: {},
      output: "object",
      handler: shareShare.bind(this),
    });
  }

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }
}

export type $ImpactLightStyle = "HEAVY" | "MEDIUM" | "LIGHT";
export type $NotificationStyle = "SUCCESS" | "WARNING" | "ERROR";
