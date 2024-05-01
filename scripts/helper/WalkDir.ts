import node_fs from "node:fs";
import node_os from "node:os";
import node_path from "node:path";
import { fileURLToPath } from "node:url";
import { IgnoreGlob } from "./IgnoreGlob.ts";
const isWindow = node_os.platform() === "win32";
export const normalizeFilePath = (filepath: string) => {
  if (filepath.startsWith("file:")) {
    filepath = fileURLToPath(filepath);
  }
  filepath = node_path.normalize(filepath);
  if (isWindow) {
    filepath = filepath.replaceAll("\\", "/");
  }
  return filepath;
};
export type WalkOptions = {
  ignore?: string | string[] | ((entry: $WalkEntry) => boolean);
  workspace?: string;
  deepth?: number;
  self?: boolean;
  log?: boolean;
};

export type $WalkEntry = FileEntry | DirectoryEntry;

abstract class Entry {
  constructor(
    readonly rootpath: string,
    readonly workspace: string,
    readonly entrypath: string,
    readonly dirpath = normalizeFilePath(node_path.dirname(entrypath)),
    readonly entryname = normalizeFilePath(node_path.basename(entrypath))
  ) {
    this.relativepath = normalizeFilePath(node_path.relative(rootpath, entrypath));
    this.relativedirpath = normalizeFilePath(node_path.relative(rootpath, dirpath));
    this.workspacepath = normalizeFilePath(node_path.relative(workspace, entrypath));
    this.workspacedirpath = normalizeFilePath(node_path.relative(workspace, dirpath));
  }
  readonly relativepath;
  readonly relativedirpath;
  readonly workspacepath;
  readonly workspacedirpath;
}
export class FileEntry extends Entry {
  readonly isFile = true as const;
  readonly isDirectory = false as const;
  readText() {
    return node_fs.readFileSync(this.entrypath, "utf-8");
  }
  readJson<T>() {
    return JSON.parse(this.readText()) as T;
  }
  read() {
    return node_fs.readFileSync(this.entrypath);
  }
  write(content: string | Uint8Array) {
    return node_fs.writeFileSync(this.entrypath, content);
  }
  writeJson(json: unknown, space?: number) {
    return this.write(JSON.stringify(json, null, space));
  }
  updateText(updater: (content: string) => string) {
    const oldContent = this.readText();
    const newContent = updater(oldContent);
    if (newContent !== oldContent) {
      this.write(newContent);
    }
  }
}
export class DirectoryEntry extends Entry {
  readonly isFile = false as const;
  readonly isDirectory = true as const;
}
const genEntry = (
  rootpath: string,
  workspace: string,
  ignore: (entry: $WalkEntry) => boolean,
  entrypath: string,
  dirpath = node_path.dirname(entrypath),
  entryname = node_path.basename(entrypath)
) => {
  if (entryname === ".DS_Store") {
    return;
  }
  let stats: node_fs.Stats;
  try {
    stats = node_fs.statSync(entrypath);
  } catch {
    /// 有可能是空的symbol-link
    return;
  }

  const isDirectory = stats.isDirectory();
  const isFile = stats.isFile();

  let entry: $WalkEntry | undefined;
  if (isFile) {
    entry = new FileEntry(rootpath, workspace, entrypath, dirpath, entryname);
  } else if (isDirectory) {
    entry = new DirectoryEntry(rootpath, workspace, entrypath, dirpath, entryname);
  }
  if (entry && ignore(entry)) {
    return;
  }
  return entry;
};
export function* WalkAny(rootpath: string, options: WalkOptions = {}) {
  rootpath = normalizeFilePath(rootpath);
  const { workspace = rootpath, deepth = Infinity, self = false, log = false } = options;
  const ignore = options.ignore
    ? typeof options.ignore === "function"
      ? options.ignore
      : (() => {
          const ignore = new IgnoreGlob(
            typeof options.ignore === "string" ? [options.ignore] : options.ignore,
            workspace
          );
          return (entry: Entry) => ignore.isIgnore(entry.entrypath);
        })()
    : () => false;

  if (log) {
    console.log("start", rootpath);
  }
  if (self) {
    const rootEntry = genEntry(rootpath, workspace, ignore, rootpath);
    if (rootEntry) {
      yield rootEntry;
    } else {
      return;
    }
  }
  const dirs = [rootpath];
  for (const dirpath of dirs) {
    /// 在被yiled后，可能会被删除
    try {
      if (node_fs.statSync(rootpath).isDirectory() !== true) {
        return;
      }
    } catch {
      return;
    }
    if (deepth !== Infinity) {
      const relativedirpath = node_path.relative(dirpath, rootpath);
      const dirDeepth = relativedirpath === "" ? 0 : relativedirpath.split("/").length;
      // console.log(dirpath, dirDeepth);
      if (dirDeepth >= deepth) {
        continue;
      }
    }

    if (!node_fs.existsSync(dirpath)) {
      continue;
    }

    for (const entryname of node_fs.readdirSync(dirpath)) {
      const entry = genEntry(rootpath, workspace, ignore, node_path.join(dirpath, entryname), dirpath, entryname);
      if (!entry) {
        continue;
      }
      yield entry;
      if (entry.isDirectory) {
        dirs.push(entry.entrypath);
      }
    }
  }
}
export function* WalkFiles(rootpath: string, options?: WalkOptions) {
  for (const entry of WalkAny(rootpath, options)) {
    if (entry.isFile) {
      yield entry;
    }
  }
}

export function* WalkDirs(rootpath: string, options?: WalkOptions) {
  for (const entry of WalkAny(rootpath, options)) {
    if (entry.isDirectory) {
      yield entry;
    }
  }
}
