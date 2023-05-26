import { SyncTask } from "./helper/SyncTask.ts";
import { syncTask as androidSyncTask } from "./sync-android.ts";
import { syncTask as desktopSyncTask, syncBrowserTask } from "./sync-desktop.ts";
import { syncTask as nextSyncTask } from "./sync-next.ts";

export const syncTask = SyncTask.concat(
  /// 1
  desktopSyncTask,
  /// 2
  androidSyncTask,
  syncBrowserTask,
  nextSyncTask
);
if (import.meta.main) {
  syncTask.auto();
}
