import { SyncTask } from "./helper/SyncTask.ts";
import { androidSyncTask } from "./sync-android.ts";
// import { syncServerTask } from "./sync-desktop.ts";
import { nextSyncTask } from "./sync-next.ts";

export const syncTask = SyncTask.concat(
  // syncServerTask,
  androidSyncTask,
  nextSyncTask,
);
if (import.meta.main) {
  syncTask.auto();
}
