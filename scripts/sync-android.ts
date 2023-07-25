import { SyncTask } from "./helper/SyncTask.ts";
// import { syncServerTask } from "./sync-desktop.ts";

export const androidSyncTask = SyncTask.from(
  {
    from: import.meta.resolve("../desktop-dev/electron"),
    to: import.meta.resolve("../android/app/src/main"),
  },
  [{ from: "assets/browser", to: "assets/browser" }]
);
if (import.meta.main) {
  // syncServerTask.auto();
  androidSyncTask.auto();
}
