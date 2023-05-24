import { NativeMicroModule } from "../../core/micro-module.native.js";
export declare class FileNMM extends NativeMicroModule {
    mmid: "file.sys.dweb";
    _bootstrap(): Promise<void>;
    protected _shutdown(): unknown;
}
