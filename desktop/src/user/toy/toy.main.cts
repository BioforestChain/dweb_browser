import { JmmMetadata } from "../../sys/jmm/JmmMetadata.cjs";
import { JsMicroModule } from "../../sys/jmm/micro-module.js.cjs";

export const cotJMM = new JsMicroModule(
  new JmmMetadata({
    id: "toy.bfs.dweb",
    server: { root: "file:///bundle", entry: "/toy.worker.js" },
  })
);
