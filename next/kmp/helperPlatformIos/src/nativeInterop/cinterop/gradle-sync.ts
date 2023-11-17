// cd ../../../../
// ./gradlew :helperPlatformIos:clean
// ./gradlew :helperPlatformIos:cinteropSync
import { exec, runTasks } from "./util.ts";

export const doGradleSyncTask = () => {
  return runTasks(doClean, doCinteropSync);
};

const doClean = () => exec(["./gradlew", ":helperPlatformIos:clean"], "../../../../");
const doCinteropSync = () => exec(["./gradlew", ":helperPlatformIos:cinteropSync"], "../../../../");

if (import.meta.main) {
  Deno.exit(await doGradleSyncTask());
}
