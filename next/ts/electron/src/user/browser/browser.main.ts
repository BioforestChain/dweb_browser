import { JmmMetadata } from "../../sys/jmm/JmmMetadata.js";
import { JsMicroModule } from "../../sys/jmm/micro-module.js.js";

export const browserJMM = new JsMicroModule(
  new JmmMetadata({
    id: "browser.sys.dweb",
    server: { root: "file:///bundle", entry: "/browser.worker.js" },
  })
);

