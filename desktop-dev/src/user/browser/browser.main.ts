import { JmmMetadata } from "../../sys/jmm/JmmMetadata.ts";
import { JsMicroModule } from "../../sys/jmm/micro-module.js.ts";

export const browserJMM = new JsMicroModule(
  new JmmMetadata({
    id: "browser.sys.dweb",
    server: { root: "file:///sys", entry: "/browser.worker.js" },
  })
);
