import { SyncTask } from "./helper/SyncTask.ts";
import { androidSyncTask } from "./sync-android.ts";
import { desktopSyncTask, syncServerTask } from "./sync-desktop.ts";
import { nextSyncTask } from "./sync-next.ts";

export const syncTask = SyncTask.concat(
  syncServerTask,
  /// 1
  desktopSyncTask,
  /// 2
  androidSyncTask,
  nextSyncTask,
);
if (import.meta.main) {
  syncTask.auto();
}
