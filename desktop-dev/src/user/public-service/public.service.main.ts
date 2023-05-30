import { JsMMMetadata } from "../../core/micro-module.js.ts";
import { JsMicroModule } from "../../core/micro-module.js.ts";

export const publicServiceJMM = new JsMicroModule(
  new JsMMMetadata({
    id: "public.service.bfs.dweb",
    server: {
      root: "dweb:///sys",
      entry: "/bfs_worker/public.service.worker.js",
    },
  })
);
