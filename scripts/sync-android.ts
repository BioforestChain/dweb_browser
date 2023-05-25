import { SyncTask } from "./helper/SyncTask.ts";
import { syncTask as desktopSyncTask } from "./sync-desktop.ts";

export const syncTask = SyncTask.from(
  {
    from: import.meta.resolve("../desktop-dev/electron"),
    to: import.meta.resolve("../android/app/src/main"),
  },
  [{ from: "assets", to: "assets" }]
);
if (import.meta.main) {
  desktopSyncTask.auto();
  syncTask.auto();
}
