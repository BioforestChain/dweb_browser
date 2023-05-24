/// <reference types="node" />
import type { OutgoingMessage } from "http";
import type { $BootstrapContext } from "../../../../core/bootstrapContext.js";
import { NativeMicroModule } from "../../../../core/micro-module.native.js";
import type { HttpServerNMM } from "../../../http-server/http-server.js";
export declare class HapticsNMM extends NativeMicroModule {
    mmid: "haptics.sys.dweb";
    httpNMM: HttpServerNMM | undefined;
    impactLightStyle: $ImpactLightStyle;
    notificationStyle: $NotificationStyle;
    duration: number;
    waitForOperationResMap: Map<string, OutgoingMessage>;
    protected _bootstrap(context: $BootstrapContext): Promise<void>;
    private sendToUi;
    protected _shutdown(): unknown;
}
export type $ImpactLightStyle = "HEAVY" | "MEDIUM" | "LIGHT";
export type $NotificationStyle = "SUCCESS" | "WARNING" | "ERROR";
