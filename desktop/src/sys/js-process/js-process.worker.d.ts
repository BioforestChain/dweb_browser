/// <reference lib="webworker"/>

// declare module "js-proccess" {
//   global {
namespace globalThis {
  type $MMID = import("../helper/types.cjs").$MMID;
  // import { JsProcessMicroModule as JsProcessMicroModuleContructor } from "./js-process.worker.cjs";
  type JsProcessMicroModuleContructor =
    import("./js-process.worker.mjs").JsProcessMicroModule;
  export const JsProcessMicroModule: new (
    mmid: $MMID
  ) => JsProcessMicroModuleContructor;
  // export { JsProcessMicroModule } from "./js-process.worker.cjs";
  export const jsProcess: JsProcessMicroModuleContructor;
  export const http: typeof import("../http-server/$createHttpDwebServer.cjs");
  export const ipc: typeof import("../../core/ipc/index.cjs");
}
// }
