/// <reference lib="webworker"/>

namespace globalThis {
  type $MMID = import("../helper/types.cjs").$MMID;
  // import { JsProcessMicroModule as JsProcessMicroModuleContructor } from "./js-process.worker.cjs";
  type JsProcessMicroModuleContructor =
    import("./js-process.worker.cjs").JsProcessMicroModule;
  export const JsProcessMicroModule: new (
    mmid: $MMID
  ) => JsProcessMicroModuleContructor;
  // export { JsProcessMicroModule } from "./js-process.worker.cjs";
  export const jsProcess: JsProcessMicroModuleContructor;
  declare const a = 1;
  export const b = 2;
}
