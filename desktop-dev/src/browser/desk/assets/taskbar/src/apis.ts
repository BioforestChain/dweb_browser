export { exportApis } from "../../../../../helper/openNativeWindow.preload.ts";
import { importApis } from "../../../../../helper/openNativeWindow.preload.ts";
export const mainApis = importApis<import("../../../desk.nmm.ts").TaskbarApi>();
Object.assign(globalThis, { mainApis });
