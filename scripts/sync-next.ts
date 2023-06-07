import { SyncTask } from "./helper/SyncTask.ts";
// import { syncTask as desktopSyncTask } from "./sync-desktop.ts";

export const syncTask = SyncTask.from(
  {
    from: import.meta.resolve("../desktop-dev/electron"),
    to: import.meta.resolve("../next/dweb-browser/src/Resources"),
  },
  [{ from: "assets", to: "Assets" }]
);
if (import.meta.main) {
  // desktopSyncTask.auto();
  syncTask.auto();
}
