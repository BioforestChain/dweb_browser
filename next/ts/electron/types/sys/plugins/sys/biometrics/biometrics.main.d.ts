/// <reference types="node" />
import { NativeMicroModule } from "../../../../core/micro-module.native.js";
import type { HttpServerNMM } from "../../../http-server/http-server.js";
import type { IncomingMessage, OutgoingMessage } from "http";
import type { $BootstrapContext } from "../../../../core/bootstrapContext.js";
export declare class BiometricsNMM extends NativeMicroModule {
    mmid: "biometrics.sys.dweb";
    allocId: number;
    httpNMM: HttpServerNMM | undefined;
    impactLightStyle: $ImpactLightStyle;
    notificationStyle: $NotificationStyle;
    duration: number;
    waitForOperationResMap: Map<string, OutgoingMessage>;
    encoder: TextEncoder;
    reqResMap: Map<number, $ReqRes>;
    protected _bootstrap(context: $BootstrapContext): Promise<void>;
    private getIdFromReq;
    protected _shutdown(): unknown;
}
export type $ImpactLightStyle = "HEAVY" | "MEDIUM" | "LIGHT";
export type $NotificationStyle = "SUCCESS" | "WARNING" | "ERROR";
export interface $ReqRes {
    req: IncomingMessage;
    res: OutgoingMessage;
}
