import { SyncTask } from "./helper/SyncTask.ts";
import { syncTask as desktopSyncTask } from "./sync-desktop.ts";

export const syncTask = SyncTask.from(
  {
    from: import.meta.resolve("../plaoc/demo"),
    to: import.meta.resolve("../desktop-dev/electron/assets"),
  },
  [{ from: "dist", to: "cot-demo" }],
);
export const syncBrowserTask = SyncTask.from(
  {
    from: import.meta.resolve("../desktop-dev/src/user/browser/assets/browser"),
    to: import.meta.resolve("../desktop-dev/electron/assets"),
  },
  [{ from: "dist", to: "browser" }],
);
if (import.meta.main) {
  desktopSyncTask.auto();
  syncTask.auto();
  syncBrowserTask.auto();
}
