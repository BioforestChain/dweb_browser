import path from "node:path";
import { __dirname, exec } from "./util.ts";

export const doCreateXcTask = async () => {
  await Deno.remove(path.resolve(__dirname, "xcframeworks"), { recursive: true });
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
