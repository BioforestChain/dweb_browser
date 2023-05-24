import type { $MMID, $PromiseMaybe } from "../helper/types.js";
import type { MicroModule } from "./micro-module.js";
import type { $ConnectResult } from "./nativeConnect.js";
export interface $BootstrapContext {
    dns: $DnsMicroModule;
}
export interface $DnsMicroModule {
    install(mm: MicroModule): void;
    uninstall(mm: MicroModule): void;
    connect(mmid: $MMID, reason?: Request): $PromiseMaybe<$ConnectResult>;
    query(mmid: $MMID): Promise<MicroModule | undefined>;
}
