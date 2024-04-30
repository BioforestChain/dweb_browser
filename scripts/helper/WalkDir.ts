import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { IgnoreGlob } from "./IgnoreGlob.ts";
export const normalizeFilePath = (filepath: string) => {
  if (filepath.startsWith("file:")) {
    filepath = fileURLToPath(filepath);
  }
  return path.normalize(filepath);
};
export type WalkOptions = {
  ignore?: string | string[];
  workspace?: string;
  deepth?: number;
  self?: boolean;
  log?: boolean;
};
export function* WalkAny(rootpath: string, options: WalkOptions = {}) {
  rootpath = normalizeFilePath(rootpath);
  const { workspace = rootpath, deepth = Infinity, self = false, log = false } = options;
  const ignore = options.ignore
    ? (() => {
        const ignore = new IgnoreGlob(
          typeof options.ignore === "string" ? [options.ignore] : options.ignore,
          workspace
        );
        return (entrypath: string) => ignore.isIgnore(entrypath);
      })()
    : () => false;

  const genEntry = (entrypath: string, dirpath = path.dirname(entrypath), entryname = path.basename(entrypath)) => {
    if (entryname === ".DS_Store") {
      return;
    }
    let stats: fs.Stats;
    try {
      stats = fs.statSync(entrypath);
    } catch {
      /// 有可能是空的symbol-link
      return;
    }
    const isDirectory = stats.isDirectory();
    const isFile = stats.isFile();
    const relativepath = path.relative(rootpath, entrypath);
    const relativedirpath = path.relative(rootpath, dirpath);
    const workspacepath = path.relative(workspace, entrypath);
    const workspacedirpath = path.relative(workspace, dirpath);

    const entryBase = {
      entryname,
      entrypath,
      dirpath,
      rootpath,
      relativepath,
      relativedirpath,
      stats,
      workspacepath,
      workspacedirpath,
    };

    if (ignore(workspacepath)) {
      // console.log("ignored", workspacepath);
      return;
    }

    if (isFile) {
      return {
        ...entryBase,
        isFile,
        isDirectory: false as const,
        readText() {
          return fs.readFileSync(entrypath, "utf-8");
        },
        readJson<T>() {
          return JSON.parse(this.readText()) as T;
        },
        read() {
          return fs.readFileSync(entrypath);
        },
        write(content: string | Uint8Array) {
          return fs.writeFileSync(entrypath, content);
        },
        writeJson(json: unknown, space?: number) {
          return this.write(JSON.stringify(json, null, space));
        },
        updateText(updater: (content: string) => string) {
          const oldContent = this.readText();
          const newContent = updater(oldContent);
          if (newContent !== oldContent) {
            this.write(newContent);
          }
        },
      };
    }
    if (isDirectory) {
      return {
        ...entryBase,
        isDirectory,
        isFile: false as const,
      };
    }
  };

  if (log) {
    console.log("start", rootpath);
  }
  if (self) {
    const rootEntry = genEntry(rootpath);
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
      if (fs.statSync(rootpath).isDirectory() !== true) {
        return;
      }
    } catch {
      return;
    }
    if (deepth !== Infinity) {
      const relativedirpath = path.relative(dirpath, rootpath);
      const dirDeepth = relativedirpath === "" ? 0 : relativedirpath.split("/").length;
      // console.log(dirpath, dirDeepth);
      if (dirDeepth >= deepth) {
        continue;
      }
    }
    
    if(!fs.existsSync(dirpath)) {
      continue;
    }

    for (const entryname of fs.readdirSync(dirpath)) {
      const entry = genEntry(path.join(dirpath, entryname), dirpath, entryname);
      if (!entry) {
        continue;
      }
      yield entry;
      if (entry.isDirectory) {
        dirs.push(entry.entrypath);
      }
      // if (entryname === ".DS_Store") {
      //   continue;
      // }
      // const entrypath = path.join(dirpath, entryname);

      // let stats: fs.Stats;
      // try {
      //   stats = fs.statSync(entrypath);
      // } catch {
      //   /// 有可能是空的symbol-link
      //   continue;
      // }
      // const isDirectory = stats.isDirectory();
      // const isFile = stats.isFile();
      // const relativepath = path.relative(rootpath, entrypath);
      // const relativedirpath = path.relative(rootpath, dirpath);
      // const workspacepath = path.relative(workspace, entrypath);
      // const workspacedirpath = path.relative(workspace, dirpath);

      // if (ignore(workspacepath)) {
      //   // console.log("ignored", workspacepath);
      //   continue;
      // }
      // const entryBase = {
      //   entryname,
      //   entrypath,
      //   dirpath,
      //   rootpath,
      //   relativepath,
      //   relativedirpath,
      //   stats,
      //   workspacepath,
      //   workspacedirpath,
      // };

      // if (isFile) {
      //   yield {
      //     ...entryBase,
      //     isFile,
      //     isDirectory: false as const,
      //     readText() {
      //       return fs.readFileSync(entrypath, "utf-8");
      //     },
      //     readJson<T>() {
      //       return JSON.parse(this.readText()) as T;
      //     },
      //     read() {
      //       return fs.readFileSync(entrypath);
      //     },
      //     write(content: string | Uint8Array) {
      //       return fs.writeFileSync(entrypath, content);
      //     },
      //     writeJson(json: unknown, space?: number) {
      //       return this.write(JSON.stringify(json, null, space));
      //     },
      //     updateText(updater: (content: string) => string) {
      //       const oldContent = this.readText();
      //       const newContent = updater(oldContent);
      //       if (newContent !== oldContent) {
      //         this.write(newContent);
      //       }
      //     },
      //   };
      // }
      // if (isDirectory) {
      //   yield {
      //     ...entryBase,
      //     isDirectory,
      //     isFile: false as const,
      //   };
      //   dirs.push(entrypath);
      // }
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
