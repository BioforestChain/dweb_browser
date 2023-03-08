import { JmmMetadata } from "../../sys/jmm/JmmMetadata.cjs";
import { JsMicroModule } from "../../sys/jmm/micro-module.js.cjs";

export const cotDemoJMM = new JsMicroModule(
  new JmmMetadata({
    id: "cotDemo.bfs.dweb",
    server: { root: "file:///bundle", entry: "/cotDemo.worker.js" },
  })
);
