/// <reference types="node" />
import { NativeMicroModule } from "../../../../core/micro-module.native.js";
import type { OutgoingMessage } from "http";
import type { $BootstrapContext } from "../../../../core/bootstrapContext.js";
import type { HttpServerNMM } from "../../../http-server/http-server.js";
export declare class ShareNMM extends NativeMicroModule {
    mmid: "share.sys.dweb";
    httpNMM: HttpServerNMM | undefined;
    waitForOperationResMap: Map<string, OutgoingMessage>;
    protected _bootstrap(context: $BootstrapContext): Promise<void>;
    protected _shutdown(): unknown;
}
export type $ImpactLightStyle = "HEAVY" | "MEDIUM" | "LIGHT";
export type $NotificationStyle = "SUCCESS" | "WARNING" | "ERROR";
