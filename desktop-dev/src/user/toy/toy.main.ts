import { JsMMMetadata } from "../../core/micro-module.js.ts";
import { JsMicroModule } from "../../core/micro-module.js.ts";

export const cotJMM = new JsMicroModule(
  new JsMMMetadata({
    id: "toy.bfs.dweb",
    server: { root: "file:///sys", entry: "/toy.worker.js" },
  })
);
