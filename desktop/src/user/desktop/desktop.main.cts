import { JmmMetadata } from "../../sys/jmm/JmmMetadata.cjs";
import { JsMicroModule } from "../../sys/jmm/micro-module.js.cjs";

export const desktopJmm = new JsMicroModule(
  new JmmMetadata({
    id: "desktop.sys.dweb",
    server: { root: "file:///bundle", entry: "/desktop.worker.js" },
  })
);
