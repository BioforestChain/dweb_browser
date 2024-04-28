import { SyncTask } from "./helper/SyncTask.ts";
import { kmpSyncTask } from "./sync-kmp.ts";

export const syncTask = SyncTask.concat(
  // syncServerTask,
  kmpSyncTask,
  // androidSyncTask,
);
if (import.meta.main) {
  syncTask.auto();
}
