import path from "node:path";
import fs from "node:fs";
import { __dirname, exec, runTasks } from "./util.ts";

export const doCreateXcTask = async () => {
  const xcframeworksDir = path.resolve(__dirname, "xcframeworks");
  if(fs.existsSync(xcframeworksDir)) {
    await Deno.remove(xcframeworksDir, { recursive: true });
    console.log("end xcframeworksDir delete!");
  }
  return runTasks(
        () => createXc("DwebPlatformIosKit"),
        () => createXc("DwebWebBrowser"),
    );
};

const createXc = (prjectName: string) => {

  const xcarchivePath = "archives/" + prjectName + "-iOS.xcarchive";
  const xcarchiveSimulatorPath = "archives/"+ prjectName + "-iOS_Simulator.xcarchive";
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
}

if (import.meta.main) {
  Deno.exit(await doCreateXcTask());
}

