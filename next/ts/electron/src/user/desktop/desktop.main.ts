import { JmmMetadata } from "../../sys/jmm/JmmMetadata.js";
import { JsMicroModule } from "../../sys/jmm/micro-module.js.js";

export const desktopJmm = new JsMicroModule(
  new JmmMetadata({
    id: "desktop.sys.dweb",
    server: { root: "file:///bundle", entry: "/desktop.worker.js" },
  })
);
