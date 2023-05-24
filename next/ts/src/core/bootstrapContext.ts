import type { $MMID, $PromiseMaybe } from "../helper/types.ts";
import type { MicroModule } from "./micro-module.ts";
import type { $ConnectResult } from "./nativeConnect.ts";
export interface $BootstrapContext {
  dns: $DnsMicroModule;
}
export interface $DnsMicroModule {
  install(mm: MicroModule): void;
  uninstall(mm: MicroModule): void;
  connect(mmid: $MMID, reason?: Request): $PromiseMaybe<$ConnectResult>;
  query(mmid: $MMID): Promise<MicroModule | undefined>
}
