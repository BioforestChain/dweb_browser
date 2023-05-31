import fs from "node:fs";
import path from "node:path";
export function* WalkAny(rootpath: string) {
  const dirs = [rootpath];
  for (const dirpath of dirs) {
    for (const entryname of fs.readdirSync(dirpath)) {
      if (entryname === ".DS_Store") {
        continue;
      }
      const entrypath = path.join(dirpath, entryname);
      const stats = fs.statSync(entrypath);
      const isDirectory = stats.isDirectory();
      const isFile = stats.isFile();
      const relativepath = path.relative(rootpath, entrypath);
      const relativedirpath = path.relative(rootpath, dirpath);
      const entryBase = {
        entryname,
        entrypath,
        dirpath,
        rootpath,
        relativepath,
        relativedirpath,
        stats,
      };

      if (isFile) {
        yield {
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
        yield {
          ...entryBase,
          isDirectory,
          isFile: false as const,
        };
        dirs.push(entrypath);
      }
    }
  }
}
export function* WalkFiles(rootpath: string) {
  for (const entry of WalkAny(rootpath)) {
    if (entry.isFile) {
      yield entry;
    }
  }
}

export function* WalkDirs(rootpath: string) {
  for (const entry of WalkAny(rootpath)) {
    if (entry.isDirectory) {
      yield entry;
    }
  }
}
