import type { Ipc } from "./ipc.js";
import { BodyHub, IpcBody } from "./IpcBody.js";
import type { MetaBody } from "./MetaBody.js";
export declare class IpcBodyReceiver extends IpcBody {
    readonly metaBody: MetaBody;
    private static metaIdIpcMap;
    constructor(metaBody: MetaBody, ipc: Ipc);
    protected _bodyHub: BodyHub;
}
