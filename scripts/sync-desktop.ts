import { SyncTask } from "./helper/SyncTask.ts";
export const syncServerTask = SyncTask.from(
  {
    from: import.meta.resolve("../plaoc/cli/serve"),
    to: import.meta.resolve("../desktop-dev/electron/assets"),
  },
  [{ from: "server", to: "server" }],
);
export const desktopSyncTask = SyncTask.from(
  {
    from: import.meta.resolve("../plaoc/demo"),
    to: import.meta.resolve("../desktop-dev/electron/assets"),
  },
  [{ from: "dist", to: "plaoc-demo" }],
);

if (import.meta.main) {
  syncServerTask.auto();
  desktopSyncTask.auto();
}
