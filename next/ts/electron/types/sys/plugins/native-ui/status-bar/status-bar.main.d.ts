/// <reference types="node" />
import { NativeMicroModule } from "../../../../core/micro-module.native.js";
import type { Ipc } from "../../../../core/ipc/ipc.js";
import type { IpcRequest } from "../../../../core/ipc/IpcRequest.js";
import type { $BootstrapContext } from "../../../../core/bootstrapContext.js";
import type { IncomingMessage, OutgoingMessage } from "http";
export declare class StatusbarNativeUiNMM extends NativeMicroModule {
    mmid: "status-bar.nativeui.sys.dweb";
    httpIpc: Ipc | undefined;
    observes: Map<string /** headers.host */, Ipc>;
    observesState: Map<string /**headers.host */, boolean>;
    encoder: TextEncoder;
    _bootstrap: (context: $BootstrapContext) => Promise<void>;
    _onConnect(ipc: Ipc): void;
    _shutdown(): void;
}
export interface $Operation {
    acction: string;
    value: string;
}
export interface $StatusbarHtmlRequest {
    ipc: Ipc;
    request: IpcRequest;
    appUrl: string;
}
export declare enum $StatusbarStyle {
    light = "light",
    dark = "dark",
    default = "default"
}
export type $isOverlays = "0" | "1";
export interface $ReqRes {
    req: IncomingMessage;
    res: OutgoingMessage;
}
export interface $Observe {
    res: OutgoingMessage | undefined;
    isObserve: boolean;
}
