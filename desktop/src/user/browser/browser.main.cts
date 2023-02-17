import { resolveToRootFile } from "../../helper/createResolveTo.cjs";
import { JsMicroModule } from "../../sys/micro-module.js.cjs";

export const browserJMM = new JsMicroModule("browser.sys.dweb", {
  main_url: resolveToRootFile("bundle/browser.worker.js").href,
} as const);