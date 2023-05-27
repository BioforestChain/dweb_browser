import fs from "node:fs";
import path from "node:path";
export function* WalkDir(rootpath: string) {
  const dirs = [rootpath];
  for (const dirpath of dirs) {
    for (const filename of fs.readdirSync(dirpath)) {
      if (filename === ".DS_Store") {
        continue;
      }
      const filepath = path.join(dirpath, filename);
      const stats = fs.statSync(filepath);
      const isDirectory = stats.isDirectory();
      const isFile = stats.isFile();

      if (isFile) {
        const relativepath = path.relative(rootpath, filepath);
        yield {
          dirpath,
          filename,
          filepath,
          rootpath,
          relativepath,
          stats,
          isFile,
          isDirectory,
          readText() {
            return fs.readFileSync(filepath, "utf-8");
          },
          readJson<T>() {
            return JSON.parse(this.readText()) as T;
          },
          read() {
            return fs.readFileSync(filepath);
          },
          write(content: string | Uint8Array) {
            return fs.writeFileSync(filepath, content);
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
        dirs.push(filepath);
      }
    }
  }
}
