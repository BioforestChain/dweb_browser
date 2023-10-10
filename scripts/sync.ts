import { SyncTask } from "./helper/SyncTask.ts";
// import { androidSyncTask } from "./sync-android.ts";
import { kmpSyncTask } from "./sync-kmp.ts";
// import { syncServerTask } from "./sync-desktop.ts";
import { nextSyncTask } from "./sync-next.ts";

export const syncTask = SyncTask.concat(
  // syncServerTask,
  kmpSyncTask,
  // androidSyncTask,
  nextSyncTask
);
if (import.meta.main) {
  syncTask.auto();
}
