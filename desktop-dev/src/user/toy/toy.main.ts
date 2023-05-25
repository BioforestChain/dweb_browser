import { JmmMetadata } from "../../sys/jmm/JmmMetadata.ts";
import { JsMicroModule } from "../../sys/jmm/micro-module.js.ts";

export const cotJMM = new JsMicroModule(
  new JmmMetadata({
    id: "toy.bfs.dweb",
    server: { root: "file:///sys", entry: "/toy.worker.js" },
  })
);
