// #!/bin/bash

import { mapHelper } from "@dweb-browser/helper/fun/mapHelper.ts";
import * as colors from "@std/fmt/colors";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { EasySpinner } from "./scripts/helper/UploadSpinner.ts";
import { $ } from "./scripts/helper/exec.ts";
import { Input } from "./toolkit/plaoc/cli/deps/cliffy.ts";

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
const suffixs = `swift|kt|kts|java|sh|ts|mts|cts|js|json|jsonc|svelte|vue|css|scss|md`
  .split("|")
  .map((ext) => `.${ext}`);
console.log(`开始统计代码，根据文件后缀: \n${colors.green(suffixs.join(" "))}`);
const files = (await $.string(`git ls-files`))
  .split("\n")
  .filter((filepath) => suffixs.some((suffix) => filepath.endsWith(suffix)));

/**
 * 别名表
 */
const authorAlias = new Map<string, string>();
const contributors = [
  ["黄诗猛", ["Mike", "Mike.Huang"]],
  ["黄　林", ["jackie-yellow", "hl19081555"]],
  ["黄水榜", ["waterbang"]],
  ["黄剑平", ["kingsword09", "Kingsword"]],
  ["彭小华", ["pxh", "pengxiaohua575527452"]],
  ["张宏星", ["xing123456789"]],
  ["周林杰", ["Charlieatinstinct"]],
  ["柯肇丰", ["Gaubee", "kezhaofeng"]],
] as const;

contributors.forEach(([name, aliasList]) => {
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
  getSorted() {
    return [...this.statistics.values()].sort((a, b) => b.line - a.line);
  }
  getByAuthor(author: string) {
    author = authorAlias.get(author) || author;
    return mapHelper.getOrPut(this.statistics, author, () => new Contribution(author));
  }
  getLog() {
    const logs: string[] = [];
    const contributionList = this.getSorted();
    const totalLine = contributionList.reduce((lines, c) => (lines += c.line), 0);
    logs.push(`${colors.magenta(`已统计有效代码行:`)} ${colors.blue(`${totalLine}`)}`);
    for (const contribution of contributionList) {
      logs.push(contribution.getLog() + colors.magenta(` ${((contribution.line / totalLine) * 100).toFixed(2)}%`));
    }
    return logs.join("\n");
  }
}

const authorContribution = new AuthorContribution();
const spinner = new EasySpinner({ redrawInterval: 100 });

/**
 * 无效代码
 */
const invalidCode = new Set(["", "//", ..."[]{}(),;\t*"]);

const warnings: string[] = [];

const spinnerProgress = (p: number, label: string | (() => string)) => {
  spinner.text = () =>
    "\n" +
    [
      ...warnings,
      "",
      authorContribution.getLog(),
      "",
      [
        colors.blue(`${p}/${files.length}`),
        colors.gray(`(${((p / files.length) * 100).toFixed(2)}%)`),
        typeof label === "function" ? label() : label,
      ].join(" "),
    ]
      .join("\n")
      .trim() +
    "\n";
};
const spinnerProgressByFile = (p: number, file: string) => {
  spinnerProgress(p, () => {
    const parts = file.split("/");
    if (parts.length <= 3) {
      return colors.green(file);
    } else {
      const firstline = parts.slice(0, Math.ceil(parts.length / 2));
      const secondline = parts.slice(firstline.length);
      return colors.green(firstline.join("/") + "/") + "\n\t\t" + colors.green(secondline.join("/"));
    }
  });
};

for (const [index, file] of files.entries()) {
  spinnerProgressByFile(index, file);
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
      warnings.push(colors.yellow(`❗ ${colors.underline(line)}`));
    }
  }
}

spinnerProgress(files.length, `✅ 统计完成 ${files.length} 个文件！ 🎉🎉🎉`);
spinner.redraw();
spinner.stop();

if (import.meta.main) {
  const contributions = authorContribution.getSorted();
  let loop = true;
  while (loop) {
    const actions = new Map<string, () => void>();
    const addAction = (option: string, action: () => void) => {
      actions.set(`${actions.size + 1}. `.padStart(4, "0") + option, action);
    };
    addAction(colors.bold(colors.green("导出到文件")), () => {
      const outputfile = fileURLToPath(import.meta.resolve("./.contribution.txt"));
      colors.setColorEnabled(false);
      fs.writeFileSync(outputfile, contributions.map((a) => a.getDetailLog()).join("\n\n"));
      colors.setColorEnabled(true);
      console.log(
        "已经导出到文件:",
        colors.blue("./" + path.relative(fileURLToPath(import.meta.resolve("./")), outputfile))
      );
      loop = false;
    });
    contributions.forEach((contribution) => {
      addAction(colors.cyan("@" + contribution.author), () => {
        console.log(contribution.getDetailLog());
      });
    });
    addAction(colors.gray("退出"), () => {
      loop = false;
    });

    const select = await Input.prompt({
      message: "查看详情",
      list: true,
      info: true,
      suggestions: [...actions.keys()],
    });
    const action = actions.get(select.trim());
    if (action) {
      action();
    }
  }
}
