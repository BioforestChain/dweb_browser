//@ts-check
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const resolveTo = (to: string) => fileURLToPath(import.meta.resolve(to));
const easyWriteFile = (filepath: string, content: string | Uint8Array) => {
  fs.mkdirSync(path.dirname(filepath), { recursive: true });
  fs.writeFileSync(filepath, content);
};
const easyReadFile = (filepath: string) => fs.readFileSync(filepath, "utf-8");
const emptyDir = (dir: string) =>
  fs.rmSync(dir, { recursive: true, force: true });

function* WalkDir(rootpath: string) {
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
            return easyReadFile(filepath);
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

emptyDir(resolveTo("./assets-projects"));
emptyDir(resolveTo("./src"));

for (const entry of WalkDir(resolveTo("../../desktop/src"))) {
  ///  资源文件夹的东西直接迁移
  if (entry.relativepath.includes("/assets/") && false) {
    const [dir, ...rels] = entry.relativepath.split("/assets/");
    const projectName = path.basename(dir);
    console.log("assets projectName:", projectName);
    const toFile = fileURLToPath(
      import.meta.resolve(
        "./assets-projects/" + projectName + "/" + rels.join("/assets/")
      )
    );
    easyWriteFile(toFile, entry.readText());
  } else if (/\.(cts|mts|ts)$/.test(entry.filename)) {
    const toFile = fileURLToPath(
      import.meta.resolve(
        "./src/" + entry.relativepath.replace(/\.(cts|mts|ts)$/, ".ts")
      )
    );
    const scriptContent = entry.readText();

    easyWriteFile(
      toFile,
      scriptContent
        /// request
        // .replace("const")
        ///lodash
        .replace(
          /import\s+(\w+)\s+from\s+['"]lodash\/(\w+)['"]/g,
          (_, $1, $2) => {
            if ($1 === $2) {
              return `import { ${$1} } from "lodash"`;
            }
            return `import {${$2} as ${$1}} from "lodash"`;
          }
        )
        /// nodejs
        .replace(/from\s+['"](http|fs|net|stream)['"]/g, 'from "node:$1"')
        /// deno
        .replace(/(from|import) ['"]([^"']+)\.js['"]/g, '$1 "$2.ts"')
        .replace(/(from|import) ['"]([^"']+)\.cjs['"]/g, '$1 "$2.ts"')
        .replace(/(from|import) ['"]([^"']+)\.mjs['"]/g, '$1 "$2.ts"')
        .replace(/import\(['"]([^"']+)\.js['"]\)/g, 'import("$1.ts")')
        .replace(/import\(['"]([^"']+)\.cjs['"]\)/g, 'import("$1.ts")')
        .replace(/import\(['"]([^"']+)\.mjs['"]\)/g, 'import("$1.ts")')
    );
  } else {
    const toFile = fileURLToPath(
      import.meta.resolve("./src/" + entry.relativepath)
    );
    console.log("copy", entry.relativepath);
    easyWriteFile(toFile, entry.read());
  }
}

easyWriteFile(
  resolveTo("./assets-projects/electron-vite.config.ts"),
  easyReadFile(resolveTo("../../desktop/electron-vite.config.ts"))
);
