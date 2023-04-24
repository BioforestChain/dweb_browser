// @ts-check
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

/**
 *
 * @param {Set<string>} dirs
 */
function* walkDir(dirs) {
  for (const dirname of dirs) {
    for (const name of fs.readdirSync(dirname)) {
      const fullname = path.resolve(dirname, name);
      if (fs.statSync(fullname).isDirectory()) {
        dirs.add(fullname);
        continue;
      }
      const entry = {
        filename: fullname,
        dirname,
        name,
        readText: () => fs.readFileSync(fullname, "utf-8"),
        writeText: (text) => fs.writeFileSync(fullname, text),
        replaceText: (cb) => entry.writeText(cb(entry.readText())),
      };
      yield entry;
    }
  }
}

const projectDir = fileURLToPath(new URL("../dweb-browser", import.meta.url));

for (const entry of walkDir(new Set([projectDir]))) {
  if (entry.filename.endsWith(".cs")) {
    // console.log(entry.filename);
    entry.replaceText((text) =>
      text.replace(/\$"([^"]+)"/g, (code, strInter) => {
        console.log(entry.name, code);
        const names = [...code.matchAll(/\{(.+?)\}/g)];
        if (names.length > 0) {
          const keys = names.map((name) => name[1]);
          let index = 0;
          const x = strInter.replace(/\{(.+?)\}/g, (_, key) => {
            return `{${index++}}`;
          });
          const stringFormat = `String.Format("${x}", ${keys.join(", ")})`;
          console.log(code, "=>", stringFormat);
          return stringFormat;
        }
        return code;
      })
    );
  }
}
