import path from "node:path";
import fs from "node:fs";
import { __dirname, exec } from "./util.ts";

export const doCreateXcTask = async () => {
  const xcframeworksDir = path.resolve(__dirname, "xcframeworks");
  if(fs.existsSync(xcframeworksDir)) {
    await Deno.remove(xcframeworksDir, { recursive: true });
  }
  return exec([
    "xcodebuild",
    "-create-xcframework",
    "-archive",
    "archives/DwebPlatformIosKit-iOS.xcarchive",
    "-framework",
    "DwebPlatformIosKit.framework",
    "-archive",
    "archives/DwebPlatformIosKit-iOS_Simulator.xcarchive",
    "-framework",
    "DwebPlatformIosKit.framework",
    "-output",
    "xcframeworks/DwebPlatformIosKit.xcframework",
  ]);
};

if (import.meta.main) {
  Deno.exit(await doCreateXcTask());
}
