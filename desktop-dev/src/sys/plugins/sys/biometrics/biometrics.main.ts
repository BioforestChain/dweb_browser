import { NativeMicroModule } from "../../../../core/micro-module.native.ts";
import type { HttpServerNMM } from "../../../http-server/http-server.ts";

export class BiometricsNMM extends NativeMicroModule {
  mmid = "biometrics.sys.dweb" as const;
  allocId = 0;
  httpNMM: HttpServerNMM | undefined;
  impactLightStyle: $ImpactLightStyle = "HEAVY";
  notificationStyle: $NotificationStyle = "SUCCESS";
  encoder = new TextEncoder();
  // reqResMap: Map<number, $ReqRes> = new Map();

  protected _bootstrap() {
    console.log("biometrices",`[${this.mmid}] _bootstrap`);
    this.registerCommonIpcOnMessageHandler({
      pathname: "/check",
      matchMode: "full",
      input: {},
      output: "boolean",
      handler: () => {
        return true;
      },
    });

    this.registerCommonIpcOnMessageHandler({
      pathname: "/biometrics",
      matchMode: "full",
      input: {},
      output: "object",
      handler: async () => {
        return await this.nativeFetch(
          `file://mwebview.sys.dweb/plubin/biommetrices`
        );
      },
    });
  }


  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }
}

export type $ImpactLightStyle = "HEAVY" | "MEDIUM" | "LIGHT";
export type $NotificationStyle = "SUCCESS" | "WARNING" | "ERROR";
 
