import { JmmMetadata } from "../../sys/jmm/JmmMetadata.cjs";
import { JsMicroModule } from "../../sys/jmm/micro-module.js.cjs";

export const jmmtestconnectJMM = new JsMicroModule(
  new JmmMetadata({
    id: "jmm.test.connect.dweb",
    server: { root: "file:///bundle", entry: "/jmmtestconnect.worker.js" },
  })
);
