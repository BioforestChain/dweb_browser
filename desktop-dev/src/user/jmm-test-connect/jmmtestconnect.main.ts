import { JmmMetadata } from "../../sys/jmm/JmmMetadata.ts";
import { JsMicroModule } from "../../sys/jmm/micro-module.js.ts";

export const jmmtestconnectJMM = new JsMicroModule(
  new JmmMetadata({
    id: "jmm.test.connect.dweb",
    server: { root: "file:///sys", entry: "/jmmtestconnect.worker.js" },
  })
);
