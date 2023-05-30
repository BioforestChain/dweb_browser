import { JsMMMetadata } from "../../core/micro-module.js.ts";
import { JsMicroModule } from "../../core/micro-module.js.ts";

export const desktopJmm = new JsMicroModule(
  new JsMMMetadata({
    id: "desktop.sys.dweb",
    server: { root: "file:///sys", entry: "/desktop.worker.js" },
  })
);
