// ./build.sh
// ./gradle-sync.sh

import { doBuildTask } from "./build.ts";
import { doGradleSyncTask } from "./gradle-sync.ts";
import { runTasks } from "./util.ts";

export const doAllTask = () => runTasks(doBuildTask, doGradleSyncTask);

if (import.meta.main) {
  Deno.exit(await doAllTask());
}
