import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";
import prettier from "npm:prettier";
import { WalkFiles } from "../../../../../scripts/WalkDir.ts";

const cssFilepath = fileURLToPath(new URL("../src/pages/new-tab/components/widget.scss", import.meta.url).href);

const classAdded = new Set<string>();
let cssText = "";
if (process.argv[2] === undefined) {
  console.error(
    `请下载 https://github.com/saadeghi/daisyui/tree/master/src/docs/src/routes/components 文件夹，将文件夹路径传入 (https://download-directory.github.io/)`
  );
  process.exit(1);
}
for (const entry of WalkFiles(process.argv[2])) {
  if (entry.entryname.endsWith(".md") === false) {
    continue;
  }
  const json = entry.readText().match(/\{\[([\w\W]+?)\]\}/)?.[1];
  if (json === undefined) {
    continue;
  }
  const data = eval(`[${json}]`) as {
    type: string;
    class: string;
    desc: string;
  }[];
  data.push(
    ...[
      "primary",
      "primary-focus",
      "primary-content",

      "secondary",
      "secondary-focus",
      "secondary-content",

      "accent",
      "accent-focus",
      "accent-content",

      "neutral",
      "neutral-focus",
      "neutral-content",

      "base-100",
      "base-200",
      "base-300",
      "base-content",

      "info",
      "info-content",
      "success",
      "success-content",
      "warning",
      "warning-content",
      "error",
      "error-content",
    ]
      .map((color) => {
        return [
          { type: "text-color", class: "text-" + color, desc: "" },
          { type: "bg-color", class: "bg-" + color, desc: "" },
        ];
      })
      .flat()
  );
  const componentName = path.basename(entry.dirpath);

  cssText +=
    `//#region ${componentName}\n` +
    data
      .filter((item) => item.type !== "responsive")
      .map(
        (item) =>
          [
            item,
            `::part(${item.class}) {
              // {${item.type}} ${item.desc}
            @apply ${item.class};
          }`,
          ] as const
      )
      .map(([item, code]) => {
        const ignoreReasons = new Set<string>();
        if (classAdded.has(item.class)) {
          ignoreReasons.add("duplication");
        } else {
          classAdded.add(item.class);
        }
        if (item.type === "responsive") {
          ignoreReasons.add(item.type);
        }
        if (item.class === "collapse-close") {
          ignoreReasons.add(item.class);
        }

        if (ignoreReasons.size !== 0) {
          if (ignoreReasons.size === 1 && [...ignoreReasons][0] === "duplication") {
            return "";
          }
          return `/* <${[...ignoreReasons].join(", ")}>
          ${code}
          */`;
        }
        return code;
      })
      .join("\n") +
    "\n" +
    `//#endregion\n\n`;
}
fs.writeFileSync(
  cssFilepath,
  await prettier.format(
    `@tailwind base;
@tailwind components;
@tailwind utilities;
@tailwind variants;
.widget {
  ::part(ani) {
    transition: 500ms;
    transition-timing-function: cubic-bezier(0.32, 0.72, 0, 1);
  }
  ${cssText}
}`,
    { parser: "scss" }
  )
);
