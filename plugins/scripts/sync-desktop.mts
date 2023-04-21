import { SyncTask } from "./helper/SyncTask.mts";

const syncTask = new SyncTask(
  {
    from: import.meta.resolve("../"),
    to: import.meta.resolve("../../desktop/app/"),
  },
  [{ from: "demo/dist", to: "cot-demo" }]
);
syncTask.auto();
