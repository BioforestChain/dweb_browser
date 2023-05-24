/// <reference types="node" />
import { NativeMicroModule } from "../../../../core/micro-module.native.js";
import type { Ipc } from "../../../../core/ipc/ipc.js";
import type { $BootstrapContext } from "../../../../core/bootstrapContext.js";
import type { OutgoingMessage } from "http";
import type { HttpServerNMM } from "../../../http-server/http-server.js";
import type { $ReqRes, $Observe } from "../../native-ui/status-bar/status-bar.main.js";
export declare class BarcodeScanningNativeUiNMM extends NativeMicroModule {
    mmid: "barcode-scanning.sys.dweb";
    httpIpc: Ipc | undefined;
    httpNMM: HttpServerNMM | undefined;
    observe: Map<string, OutgoingMessage>;
    waitForOperationRes: Map<string, OutgoingMessage>;
    reqResMap: Map<number, $ReqRes>;
    observeMap: Map<string, $Observe>;
    encoder: TextEncoder;
    allocId: number;
    _bootstrap: (context: $BootstrapContext) => Promise<void>;
    private _process;
    private _processOPTIONS;
    private _processPOST;
    private _stop;
    private _getPhoto;
    private _waitForOperation;
    private _operationReturn;
    _shutdown(): void;
}
