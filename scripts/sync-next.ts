import { SyncTask } from "./helper/SyncTask.ts";
// import { desktopSyncTask } from "./sync-desktop.ts";

export const nextSyncTask = SyncTask.from(
  {
    from: import.meta.resolve("../desktop-dev/electron"),
    to: import.meta.resolve("../next/dweb-browser/src/Resources"),
  },
  [{ from: "assets/browser", to: "Assets/browser" }]
);
if (import.meta.main) {
  // desktopSyncTask.auto();
  nextSyncTask.auto();
}
