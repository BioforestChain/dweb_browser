import fs from "node:fs";
import path from "node:path";
import { __dirname, exec, runTasks } from "./util.ts";

export const doCreateXcItemTask = (fw: string) => async () => {
  await deleteCache(fw);
  return runTasks(() => createXc(fw));
};

const deleteCache = async (fw: string) => {
  const xcframeworksDir = path.resolve(__dirname, "xcframeworks/" + fw + ".xcframework");
  if (fs.existsSync(xcframeworksDir)) {
    await Deno.remove(xcframeworksDir, { recursive: true });
    console.log("end xcframeworksDir delete!");
  }
};

const createXc = (prjectName: string) => {
  const xcarchivePath = "archives/" + prjectName + "-iOS.xcarchive";
  const xcarchiveSimulatorPath = "archives/" + prjectName + "-iOS_Simulator.xcarchive";
  const frameworkName = prjectName + ".framework";
  const xcframeworkPath = "xcframeworks/" + prjectName + ".xcframework";

  return exec([
    "xcodebuild",
    "-create-xcframework",
    "-archive",
    xcarchivePath,
    "-framework",
    frameworkName,
    "-archive",
    xcarchiveSimulatorPath,
    "-framework",
    frameworkName,
    "-output",
    xcframeworkPath,
  ]);
};

if (import.meta.main) {
  await doCreateXcItemTask("DwebPlatformIosKit")();
  await doCreateXcItemTask("DwebWebBrowser")();
  Deno.exit();
}
