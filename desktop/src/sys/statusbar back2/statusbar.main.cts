import { resolveToRootFile } from "../../helper/createResolveTo.cjs";
import { JsMicroModule } from "../../sys/micro-module.js.cjs";

export const statusbarJMM = new JsMicroModule("statusbar.sys.dweb", {
  main_url: resolveToRootFile("bundle/statusbar.worker.js").href,
} as const);
