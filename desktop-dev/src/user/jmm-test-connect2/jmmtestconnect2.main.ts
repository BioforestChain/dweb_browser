import { JsMMMetadata } from "../../core/micro-module.js.ts";
import { JsMicroModule } from "../../core/micro-module.js.ts";

export const jmmtestconnectJMM2 = new JsMicroModule(
  new JsMMMetadata({
    id: "jmm.test.connect.2.dweb",
    server: { root: "file:///sys", entry: "/jmmtestconnect2.worker.js" },
  })
);
