import { JmmMetadata } from "../../sys/jmm/JmmMetadata.cjs";
import { JsMicroModule } from "../../sys/jmm/micro-module.js.cjs";

export const browserJMM = new JsMicroModule(
  new JmmMetadata({
    id: "browser.sys.dweb",
    main_url: "file:///bundle/browser.worker.js",
  })
);
