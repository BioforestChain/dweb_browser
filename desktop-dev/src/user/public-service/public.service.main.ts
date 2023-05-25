import { JmmMetadata } from "../../sys/jmm/JmmMetadata.ts";
import { JsMicroModule } from "../../sys/jmm/micro-module.js.ts";

export const publicServiceJMM = new JsMicroModule(
  new JmmMetadata({
    id: "public.service.bfs.dweb",
    server: {
      root: "dweb:///sys",
      entry: "/bfs_worker/public.service.worker.js",
    },
  })
);
