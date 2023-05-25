import { SyncTask } from "./helper/SyncTask.ts";
import { syncTask as androidSyncTask } from "./sync-android.ts";
import { syncTask as desktopSyncTask } from "./sync-desktop.ts";
import { syncTask as nextSyncTask } from "./sync-next.ts";

export const syncTask = SyncTask.concat(
  androidSyncTask,
  nextSyncTask,
  desktopSyncTask
);
if (import.meta.main) {
  syncTask.auto();
}
