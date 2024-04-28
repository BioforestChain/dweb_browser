import { SyncTask } from "./helper/SyncTask.ts";
import { SyncJsProcessTask } from "./sync-kmp.ts";

export const syncTask = SyncTask.concat(
  // syncServerTask,
  SyncJsProcessTask
  // androidSyncTask,
);
if (import.meta.main) {
  syncTask.auto();
}
