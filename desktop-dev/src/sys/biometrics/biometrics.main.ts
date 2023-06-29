import { NativeMicroModule } from "../../core/micro-module.native.ts";

export class BiometricsNMM extends NativeMicroModule {
  mmid = "biometrics.sys.dweb" as const;

  protected _bootstrap() {
    console.log("biometrices", `[${this.mmid}] _bootstrap`);
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
          `file://mwebview.browser.dweb/plubin/biommetrices`
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
