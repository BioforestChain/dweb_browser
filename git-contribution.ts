// #!/bin/bash

import { mapHelper } from "@dweb-browser/helper/fun/mapHelper.ts";
import * as colors from "@std/fmt/colors";
import { EasySpinner } from "./scripts/helper/UploadSpinner.ts";
import { $ } from "./scripts/helper/exec.ts";

// # # 获取所有源代码文件的列表，可以根据实际情况调整文件扩展名
// # FILES=$(find . -name "*.swift" -o -name "*.kt" -o -name "*.java" -o -name "*.sh" -o -name "*.ts" -o -name "*.mts" -o -name "*.cts" -o -name "*.js" -o -name "*.json" -o -name "*.jsonc" -o -name "*.svelte" -o -name "*.vue" -o -name "*.css" -o -name "*.scss" -o -name "*.xml" -o -name "*.svg")

// # # 对每个文件运行 git blame，然后统计每个作者的行数
// # for FILE in $FILES; do
// #     git blame --show-name --show-email --line-porcelain "$FILE" | \
// #         awk '/^author/ {print $2}' | \
// #         sort | uniq -c | \
// #         while read count author; do
// #             echo "$author: $count"
// #         done
// # done

// # git ls-files | xargs -n 1 -I{} git blame -w {} | awk '{print $2}' | sort | uniq -c | sort -nr

// # git ls-files | egrep '\.(swift|kt|java|sh|ts|mts|cts|js|json|jsonc|svelte|vue|css|scss|md)$' | xargs -n 1 -I{} git blame -w {} | awk '{print $2}' | sort | uniq -c | sort -nr

// # git ls-files | egrep '\.(swift|kt|java|sh|ts|mts|cts|js|json|jsonc|svelte|vue|css|scss|md)$' | xargs -L 1 -I{} sh -c 'git blame -w "{}" | grep "pxh" > /dev/null && echo "{}"' | sort | uniq
$.cd(import.meta.resolve("./"));
const suffixs = `swift|kt|java|sh|ts|mts|cts|js|json|jsonc|svelte|vue|css|scss|md`.split("|").map((ext) => `.${ext}`);
const files = (await $.string(`git ls-files`))
  .split("\n")
  .filter((filepath) => suffixs.some((suffix) => filepath.endsWith(suffix)));

const authorAlias = new Map<string, string>();
(
  [
    ["柯肇丰", ["Gaubee", "kezhaofeng"]],
    ["黄诗猛", ["Mike", "Mike.Huang"]],
    ["黄　林", ["jackie-yellow", "hl19081555"]],
    ["黄水榜", ["waterbang"]],
    ["黄剑平", ["kingsword09", "Kingsword"]],
    ["彭小华", ["pxh", "pengxiaohua575527452"]],
    ["张宏星", ["xing123456789"]],
    ["周林杰", ["Charlieatinstinct"]],
  ] as const
).forEach(([name, aliasList]) => {
  aliasList.forEach((alias) => {
    authorAlias.set(alias, name);
  });
});

class Contribution {
  constructor(readonly author: string) {}
  readonly files = new Map<string, number>();
  public line = 0;
  addLineByFile(file: string, line: number = 1) {
    const old = this.files.get(file) || 0;
    this.files.set(file, old + line);
    this.line += line;
  }
  getLog() {
    return `${colors.cyan(this.author)} 贡献代码 ${colors.blue(this.line + "")} 行`;
  }
  getDetailLog() {
    const logs: string[] = [this.getLog()];
    this.files.forEach((line, filepath) => {
      logs.push(`\t${colors.gray(filepath)} ${colors.green(`+${line}`)}`);
    });
    return logs.join("\n");
  }
}
class AuthorContribution {
  readonly statistics = new Map<string, Contribution>();
  getByAuthor(author: string) {
    author = authorAlias.get(author) || author;
    return mapHelper.getOrPut(this.statistics, author, () => new Contribution(author));
  }
  getLog() {
    const logs: string[] = [];

    for (const contribution of [...this.statistics.values()].sort((a, b) => b.line - a.line)) {
      logs.push(contribution.getLog());
    }
    return logs.join("\n");
  }
}

const authorContribution = new AuthorContribution();
const spinner = new EasySpinner();

/**
 * 无效代码
 */
const invalidCode = new Set(["", "//", ..."[]{}(),;\t*"]);

// const easyPool = new (class EasyPool {
//   poolSize = 10; // 默认并发数为10
//   private busy = 0;
//   private waiters: PromiseOut<void>[] = [];
//   async requestWorker<R>(work: () => R) {
//     if (this.busy >= this.poolSize) {
//       const waiter = new PromiseOut<void>();
//       this.waiters.push(waiter);
//       await waiter.promise;
//     }
//     this.busy += 1;
//     try {
//       return await work();
//     } finally {
//       this.busy -= 1;
//       this.waiters.shift()?.resolve();
//     }
//   }
// })();
spinner.text = `${0}/${files.length}(0.00%)`;

for (const [index, file] of files.entries()) {
  for (const line of (await $.string(["git", "blame", "-w", file], { silent: true })).trim().split("\n")) {
    const match = line.match(/.+? \(([^\s]+) .+?\)(.*)/);
    if (match) {
      const author = match[1];
      if (!authorAlias.get(author)) {
        if (line.includes("Not Committed Yet")) {
          continue;
          // 未提交的代码
        } else {
          console.log(colors.red("未知作者"), colors.cyan(author), colors.gray(line));
        }
      }
      const code = match[2].trim();
      const contribution = authorContribution.getByAuthor(author);
      const isInvalidCode =
        code.startsWith("//") ||
        code.startsWith("/*") ||
        code.startsWith("* ") ||
        code.endsWith("*/") ||
        false === invalidCode.has(code);

      contribution.addLineByFile(file, isInvalidCode ? 0 : 1);
    } else if (line) {
      console.log("BBB", line.length, line);
    }
  }
  spinner.text = `${index + 1}/${files.length}(${(((index + 1) / files.length) * 100).toFixed(2)}%)`;
}

spinner.stop();

if (import.meta.main) {
  if (Deno.args.length > 0) {
    for (const author of Deno.args) {
      const contribution = authorContribution.getByAuthor(author);
      if (contribution.line > 0) {
        console.log(contribution.getDetailLog());
      }
    }
  } else {
    console.log(authorContribution.getLog());
  }
}
