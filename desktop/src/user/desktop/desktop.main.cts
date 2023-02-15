import { resolveToRootFile } from "../../helper/createResolveTo.cjs";
import { JsMicroModule } from "../../sys/micro-module.js.cjs";

export const desktopJmm = new JsMicroModule("desktop.sys.dweb", {
  main_url: resolveToRootFile("bundle/desktop.worker.js").href,
} as const);
