import { JmmMetadata } from "../../sys/jmm/JmmMetadata.cjs";
import { JsMicroModule } from "../../sys/jmm/micro-module.js.cjs";

export const jmmtestconnectJMM2 = new JsMicroModule(
  new JmmMetadata({
    id: "jmm.test.connect.2.dweb",
    server: { root: "file:///bundle", entry: "/jmmtestconnect2.worker.js" },
  })
);
