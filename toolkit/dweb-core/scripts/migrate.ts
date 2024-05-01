//@ts-check
import fs from "node:fs";
import node_path from "node:path";
import { fileURLToPath } from "node:url";
import { WalkFiles } from "../../../scripts/helper/WalkDir.ts";

const resolveTo = (to: string) => fileURLToPath(import.meta.resolve(to));
const easyWriteFile = (filepath: string, content: string | Uint8Array) => {
  fs.mkdirSync(node_path.dirname(filepath), { recursive: true });
  fs.writeFileSync(filepath, content);
};
const easyReadFile = (filepath: string) => fs.readFileSync(filepath, "utf-8");
const emptyDir = (dir: string) => fs.rmSync(dir, { recursive: true, force: true });

emptyDir(resolveTo("./assets-projects"));
emptyDir(resolveTo("./src"));

for (const entry of WalkFiles(resolveTo("../../desktop/src"))) {
  ///  资源文件夹的东西直接迁移
  // deno-lint-ignore no-constant-condition
  if (entry.relativepath.includes("/assets/") && false) {
    const [dir, ...rels] = entry.relativepath.split("/assets/");
    const projectName = node_path.basename(dir);
    console.log("assets projectName:", projectName);
    const toFile = fileURLToPath(import.meta.resolve("./assets-projects/" + projectName + "/" + rels.join("/assets/")));
    easyWriteFile(toFile, entry.readText());
  } else if (/\.(cts|mts|ts)$/.test(entry.entryname)) {
    const toFile = fileURLToPath(import.meta.resolve("./src/" + entry.relativepath.replace(/\.(cts|mts|ts)$/, ".ts")));
    const scriptContent = entry.readText();

    easyWriteFile(
      toFile,
      scriptContent
        /// request
        // .replace("const")
        ///lodash
        .replace(/import\s+(\w+)\s+from\s+['"]lodash\/(\w+)['"]/g, (_, $1, $2) => {
          if ($1 === $2) {
            return `import { ${$1} } from "lodash"`;
          }
          return `import {${$2} as ${$1}} from "lodash"`;
        })
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
    const toFile = fileURLToPath(import.meta.resolve("./src/" + entry.relativepath));
    console.log("copy", entry.relativepath);
    easyWriteFile(toFile, entry.read());
  }
}
