import { parseArgs } from "@std/cli/parse-args";
export const cliArgs = parseArgs(Deno.args);

import fs from "node:fs";
import { createBaseResolveTo } from "../../scripts/helper/resolveTo.ts";
const resolveTo = createBaseResolveTo(import.meta.url);
export const loadProperties = (filepath: string) => {
  const properties = new Map<string, string>();
  fs.readFileSync(filepath, "utf-8")
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line.startsWith("#") === false && line !== "")
    .forEach((line) => {
      const splitIndex = line.indexOf("=");
      if (splitIndex !== -1) {
        const key = line.slice(0, splitIndex).trim();
        let value = line.slice(splitIndex + 1).trim();
        if (value.startsWith(`"`) && value.endsWith(`"`)) {
          value = value.slice(1, -1);
        }
        properties.set(key, value);
      }
    });
  return Object.assign(properties, {
    getBoolean: (key: string) => {
      return properties.get(key) === "true";
    },
  });
};
export const localProperties = loadProperties(resolveTo("./local.properties"));

import { Glob } from "../../scripts/helper/Glob.ts";

const allTasks = new Map<string, () => unknown>();
export function defineTask(id: string, action: () => unknown) {
  allTasks.set(id, action);
}
export function listTasks(filter?: string) {
  const allTaskIds = [...[...allTasks.keys()].entries()];
  let taskIds: typeof allTaskIds;
  if (filter) {
    const match = new Glob([filter], "/");
    taskIds = allTaskIds.filter(([_, taskId]) => match.isMatch(taskId));
  } else {
    taskIds = allTaskIds;
  }
  console.log(taskIds.map(([index, taskId]) => `${index + 1}. ${taskId}`).join("\n"));
}
export function tryExecTask() {
  queueMicrotask(async () => {
    if (cliArgs.list === true) {
      listTasks();
      return;
    }
    if (typeof cliArgs.list === "string") {
      listTasks(cliArgs.list);
      return;
    }
    const allTaskIds = [...allTasks.keys()];

    const idFilters = cliArgs._.map((filter) => {
      if (typeof filter === "string") {
        return filter;
      } else if (typeof filter === "number") {
        return allTaskIds[filter - 1];
      }
      return;
    }).filter((it) => typeof it === "string") as string[];
    if (idFilters.length === 0) {
      console.warn("请输入 taskId 或者 taskId-glob 或者 taskId-序号");
      listTasks();
      return;
    }

    const match = new Glob(idFilters, "/");
    const taskIds = allTaskIds.filter((taskId) => match.isMatch(taskId));

    if (taskIds.length === 0) {
      console.warn("没有找到匹配的任务", ...idFilters);
      return;
    }
    for (const taskId of taskIds) {
      console.log("开始执行任务", taskId);
      try {
        await allTasks.get(taskId)!();
      } catch (e) {
        console.error("QWQ", e);
        Deno.exit(1);
      }
    }
    console.log("done");
  });
}
