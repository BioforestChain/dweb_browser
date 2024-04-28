import { SyncTask } from "./helper/SyncTask.ts";
// import { syncServerTask } from "./sync-desktop.ts";

export const kmpSyncTask = SyncTask.from(
  {
    from: import.meta.resolve("../toolkit/electron"),
    to: import.meta.resolve("../next/kmp/shared/src/commonMain/resources"),
  },
  [{ from: "assets/browser", to: "browser" }]
);
if (import.meta.main) {
  // syncServerTask.auto();
  kmpSyncTask.auto();
}
