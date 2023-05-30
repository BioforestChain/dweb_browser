import { JsMMMetadata } from "../../core/micro-module.js.ts";
import { JsMicroModule } from "../../core/micro-module.js.ts";

export const jmmtestconnectJMM = new JsMicroModule(
  new JsMMMetadata({
    id: "jmm.test.connect.dweb",
    server: { root: "file:///sys", entry: "/jmmtestconnect.worker.js" },
  })
);
