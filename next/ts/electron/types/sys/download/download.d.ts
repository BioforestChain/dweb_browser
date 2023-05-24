/// <reference types="node" />
import { NativeMicroModule } from "../../core/micro-module.native.js";
import type { OutgoingMessage } from "http";
import type { $BootstrapContext } from "../../core/bootstrapContext.js";
import type { HttpServerNMM } from "../http-server/http-server.js";
export declare class DownloadNMM extends NativeMicroModule {
    mmid: "download.sys.dweb";
    httpNMM: HttpServerNMM | undefined;
    waitForOperationResMap: Map<string, OutgoingMessage>;
    protected _bootstrap(context: $BootstrapContext): Promise<void>;
    protected _shutdown(): unknown;
}
