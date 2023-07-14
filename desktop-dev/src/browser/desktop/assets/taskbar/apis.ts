export { exportApis } from "../../../../helper/openNativeWindow.preload.ts";
import { importApis } from "../../../../helper/openNativeWindow.preload.ts";
export const mainApis = importApis<import("../../desktop.nmm.ts").TaskbarMainApis>();
Object.assign(globalThis, { mainApis });
