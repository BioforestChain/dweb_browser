import { JsMicroModule } from "../../sys/micro-module.js.cjs";

export const desktopJmm = new JsMicroModule("desktop.sys.dweb", {
  main_url: "bundle/desktop.worker.js",
} as const);
