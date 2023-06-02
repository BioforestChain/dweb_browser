// haptics.sys.dweb
import { NativeMicroModule } from "../../core/micro-module.native.ts";
import type { HttpServerNMM } from "../http-server/http-server.ts";
import { setHaptics } from "./handlers.ts";

export class HapticsNMM extends NativeMicroModule {
  mmid = "haptics.sys.dweb" as const;
  httpNMM: HttpServerNMM | undefined;
  impactLightStyle: $ImpactLightStyle = "HEAVY";
  notificationStyle: $NotificationStyle = "SUCCESS";

  protected _bootstrap() {
    console.always(`[${this.mmid}] _bootstrap`);
    // haptics.sys.dweb/impactLight?X-Dweb-Host=api.browser.dweb%3A443&style=HEAVY
    this.registerCommonIpcOnMessageHandler({
      pathname: "/impactLight",
      matchMode: "full",
      input: { style: "string" },
      output: "object",
      handler: setHaptics.bind(this),
    });

    // /haptics.sys.dweb/notification?X-Dweb-Host=api.browser.dweb%3A443&style=SUCCESS | WARNING | ERROR
    this.registerCommonIpcOnMessageHandler({
      pathname: "/notification",
      matchMode: "full",
      input: {},
      output: "object",
      handler: setHaptics.bind(this),
    });

    // haptics.sys.dweb/vibrateClick
    this.registerCommonIpcOnMessageHandler({
      pathname: "/vibrateClick",
      matchMode: "full",
      input: {},
      output: "object",
      handler: setHaptics.bind(this),
    });

    // /haptics.sys.dweb/vibrateDisabled
    this.registerCommonIpcOnMessageHandler({
      pathname: "/vibrateDisabled",
      matchMode: "full",
      input: {},
      output: "object",
      handler: setHaptics.bind(this),
    });

    // /haptics.sys.dweb/vibrateDoubleClick
    this.registerCommonIpcOnMessageHandler({
      pathname: "/vibrateDoubleClick",
      matchMode: "full",
      input: {},
      output: "object",
      handler: setHaptics.bind(this),
    });

    // /haptics.sys.dweb/vibrateHeavyClick
    this.registerCommonIpcOnMessageHandler({
      pathname: "/vibrateHeavyClick",
      matchMode: "full",
      input: {},
      output: "object",
      handler: setHaptics.bind(this),
    });

    // /haptics.sys.dweb/vibrateTick
    this.registerCommonIpcOnMessageHandler({
      pathname: "/vibrateTick",
      matchMode: "full",
      input: {},
      output: "object",
      handler: setHaptics.bind(this),
    });

    // /haptics.sys.dweb/customize?X-Dweb-Host=api.browser.dweb%3A443&duration=300
    this.registerCommonIpcOnMessageHandler({
      pathname: "/customize",
      matchMode: "full",
      input: {},
      output: "object",
      handler: setHaptics.bind(this),
    });
  }

  protected _shutdown(): unknown {
    throw new Error("Method not implemented.");
  }
}

export type $ImpactLightStyle = "HEAVY" | "MEDIUM" | "LIGHT";
export type $NotificationStyle = "SUCCESS" | "WARNING" | "ERROR";
