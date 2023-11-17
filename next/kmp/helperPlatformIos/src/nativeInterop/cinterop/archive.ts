import { exec, runTasks } from "./util.ts";

export const doArchiveTask = () =>
  runTasks(
    () => buildArchive("generic/platform=iOS", "archives/DwebPlatformIosKit-iOS"),
    () => buildArchive("generic/platform=iOS Simulator", "archives/DwebPlatformIosKit-iOS_Simulator")
  );

const buildArchive = (destination: string, archivePath: string) =>
  exec([
    "xcodebuild",
    "archive",
    "-project",
    "DwebPlatformIosKit.xcodeproj",
    "-scheme",
    "DwebPlatformIosKit",
    "-destination",
    destination,
    "-archivePath",
    archivePath,
    "SKIP_INSTALL=NO",
    "BUILD_LIBRARY_FOR_DISTRIBUTION=YES",
  ]);

if (import.meta.main) {
  Deno.exit(await doArchiveTask());
}
