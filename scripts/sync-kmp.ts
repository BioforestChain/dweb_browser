import { SyncTask } from "./helper/SyncTask.ts";

export const SyncJsProcessTask = SyncTask.from(
  {
    from: import.meta.resolve("../npm/@dweb-browser__js-process"),
    to: import.meta.resolve("../next/kmp/shared/src/commonMain/resources"),
  },
  [{ from: "esm", to: "js-process" }]
);
if (import.meta.main) {
  SyncJsProcessTask.auto();
}
