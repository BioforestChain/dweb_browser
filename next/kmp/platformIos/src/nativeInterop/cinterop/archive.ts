import {sourceCodeDir, exec, runTasks} from "./util.ts";
import path from "node:path"

export const doArchiveItemTask = (fw: string) =>
  () => runTasks(
    () => buildArchive(fw, sourceCodeDir + fw + "/", "generic/platform=iOS", "archives/" + fw + "-iOS"),
    () => buildArchive(fw, sourceCodeDir + fw + "/", "generic/platform=iOS Simulator", "archives/" + fw + "-iOS_Simulator"),
  );

const buildArchive = (prjectName: string, dir: string, destination: string, archivePath: string) =>
  exec([
    "xcodebuild",
    //"clean", 生成单个fw时，clean会把其他fw的产物全部删掉。会导致编译失败disk IO error。先注释掉。
    "-quiet",
    "archive",
    "-workspace",
    path.resolve(dir, "../DwebBrowser.xcworkspace"),
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
