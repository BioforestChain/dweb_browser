import { JsMMMetadata } from "../../core/micro-module.js.ts";
import { JsMicroModule } from "../../core/micro-module.js.ts";

export const browserJMM = new JsMicroModule(
  new JsMMMetadata({
    id: "browser.sys.dweb",
    server: { root: "file:///sys", entry: "/browser.worker.js" },
  })
);

