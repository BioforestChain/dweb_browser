import { JmmMetadata } from "../../sys/jmm/JmmMetadata.cjs";
import { JsMicroModule } from "../../sys/jmm/micro-module.js.cjs";

export const btryJMM = new JsMicroModule(
  new JmmMetadata({
    id: "btry.sys.dweb",
    server: { root: "file:///bundle", entry: "/btry.worker.js" },
  })
);
