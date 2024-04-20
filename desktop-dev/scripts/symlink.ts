import fs from "node:fs";
import { fileURLToPath } from "node:url";
const composeResourcesBrowser_dir = fileURLToPath(
  import.meta.resolve("../../next/kmp/browser/src/commonMain/composeResources/files/browser")
);
const resourceBrowser_dir = fileURLToPath(
  import.meta.resolve("../../next/kmp/browser/src/commonMain/resources/browser")
);
if (!fs.existsSync(resourceBrowser_dir)) {
  fs.symlinkSync(composeResourcesBrowser_dir, resourceBrowser_dir, "dir");
}
