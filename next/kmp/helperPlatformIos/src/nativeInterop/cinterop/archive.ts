import { sourceCodeDir, exec, runTasks } from "./util.ts";

export const doArchiveTask = () =>
  runTasks(
    () => buildArchive("DwebPlatformIosKit", sourceCodeDir + "DwebPlatformIosKit/", "generic/platform=iOS", "archives/DwebPlatformIosKit-iOS"),
    () => buildArchive("DwebPlatformIosKit", sourceCodeDir + "DwebPlatformIosKit/", "generic/platform=iOS Simulator", "archives/DwebPlatformIosKit-iOS_Simulator"),
    () => buildArchive("DwebWebBrowser", sourceCodeDir + "DwebWebBrowser/", "generic/platform=iOS", "archives/DwebWebBrowser-iOS"),
    () => buildArchive("DwebWebBrowser", sourceCodeDir + "DwebWebBrowser/", "generic/platform=iOS Simulator", "archives/DwebWebBrowser-iOS_Simulator"),
  );

const buildArchive = (prjectName: string, dir: string, destination: string, archivePath: string) =>
  exec([
    "xcodebuild",
    "archive",
    "-project",
    dir + prjectName + ".xcodeproj",
    "-scheme",
    prjectName,
    "-destination",
    destination,
    "-archivePath",
    archivePath,
    "SKIP_INSTALL=NO",
    "BUILD_LIBRARY_FOR_DISTRIBUTION=YES",
    "-quiet",
  ]);

if (import.meta.main) {
  Deno.exit(await doArchiveTask());
}
