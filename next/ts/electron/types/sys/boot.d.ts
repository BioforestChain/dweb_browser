import { NativeMicroModule } from "../core/micro-module.native.js";
export declare class BootNMM extends NativeMicroModule {
    private initMmids?;
    constructor(initMmids?: Iterable<`${string}.dweb`> | undefined);
    mmid: "boot.sys.dweb";
    private registeredMmids;
    _bootstrap(): Promise<void>;
    _shutdown(): void;
    private register;
    private unregister;
}
