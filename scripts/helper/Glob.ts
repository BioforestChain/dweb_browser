import fs from "node:fs";
import node_path from "node:path";
import ignore from "npm:ignore";
import { normalizeFilePath } from "./WalkDir.ts";

export class Glob {
  #rules;
  get rules() {
    return Object.freeze(this.#rules.slice());
  }
  #ignore;
  constructor(rules: string[], readonly cwd: string) {
    this.cwd = normalizeFilePath(cwd);
    this.#rules = rules;
    this.#ignore = ignore.default().add(rules);
  }
  static fromIgnoreFile(filepath: string) {
    filepath = normalizeFilePath(filepath);
    const rules = fs
      .readFileSync(filepath, "utf-8")
      .split("\n")
      .map((it) => it.trim())
      .filter((it) => !it.startsWith("#") && it.length > 0);
    const cwd = node_path.dirname(filepath);
    return new Glob(rules, cwd);
  }
  isMatch(filepath: string): boolean {
    filepath = normalizeFilePath(filepath);

    const relativepath = node_path.isAbsolute(filepath) ? node_path.relative(this.cwd, filepath) : filepath;
    return this.#ignore.ignores(relativepath);
  }
}

// const reg = new IgnoreGlob(
//   [
//     //
//     "assets/*",
//     "!assets/zzz/",
//   ],
//   import.meta.resolve("./")
// );
// console.log(reg.isIgnore(path.resolve(reg.cwd, "assets/xzzz/a.js")));
