import { sourceCodeDir, exec, runTasks } from "./util.ts";

export const doArchiveItemTask = (fw: string) =>
    () => runTasks(
        () => buildArchive(fw, sourceCodeDir + fw + "/", "generic/platform=iOS", "archives/" + fw + "-iOS"),
        () => buildArchive(fw, sourceCodeDir + fw + "/", "generic/platform=iOS Simulator", "archives/" + fw + "-iOS_Simulator"),
    );

const buildArchive = (prjectName: string, dir: string, destination: string, archivePath: string) =>
  exec([
    "xcodebuild",
    "-quiet",
    "archive",
    "-workspace",
    dir +"/../" + "DwebBrowser.xcworkspace",
    "-scheme",
    prjectName,
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
