import { SyncTask } from "./helper/SyncTask.mts";

const syncTask = new SyncTask(
  {
    from: import.meta.resolve("../"),
    to: import.meta.resolve("../../android/app/src/main/assets"),
  },
  [{ from: "demo/dist", to: "cot-demo" }]
);
syncTask.auto();
