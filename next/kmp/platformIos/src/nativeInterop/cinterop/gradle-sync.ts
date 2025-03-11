// cd ../../../../
// ./gradlew :platformIos:clean
// ./gradlew :platformIos:cinteropSync
import { exec, runTasks } from "./util.ts";

export const doGradleSyncTask = (): Promise<number> => {
  return runTasks(doClean, doCinteropSync);
};

const doClean = () => exec(["./gradlew", ":platformIos:clean"], "../../../../");
const doCinteropSync = () => exec(["./gradlew", ":platformIos:cinteropSync"], "../../../../");

if (import.meta.main) {
  Deno.exit(await doGradleSyncTask());
}
