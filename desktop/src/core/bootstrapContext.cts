import type { MicroModule } from "./micro-module.cjs";
export interface $BootstrapContext {
  dns: $DnsMicroModule;
}
export interface $DnsMicroModule {
  install(mm: MicroModule): void;
  uninstall(mm: MicroModule): void;
}
