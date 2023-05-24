import { BaseWWWServer } from "../plugins/base/base_www_server.js";
import type { $IpcMessage } from "../../core/ipc/const.js";
import type { Ipc } from "../../core/ipc/ipc.js";
import type { DownloadNMM } from "./download.js";
export declare class WWWServer extends BaseWWWServer<DownloadNMM> {
    constructor(nmm: DownloadNMM);
    _onRequestMore: (message: $IpcMessage, ipc: Ipc) => Promise<void>;
}
