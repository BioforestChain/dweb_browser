import path from "node:path";
import { fileURLToPath } from "node:url";
export const __dirname = path.dirname(fileURLToPath(import.meta.url));
export const sourceCodeDir : string = __dirname + "/../../../../app/iosApp/"

export const runTasks = async (...actions: Array<() => Promise<number>>) => {
  for (const action of actions) {
    console.log(`start ${action.name}`);
    const code = await action();
    console.log(`end ${action.name}[${code}]`);
    if (code !== 0) {
      return code;
    }
  }
  return 0;
};

export const exec = async (commands: string[], cwd = __dirname) => {
  const [command, ...args] = commands;
  cwd = path.resolve(__dirname, cwd);
  const task = new Deno.Command(command, {
    cwd,
    args,
    stdin: "inherit",
    stdout: "inherit",
  }).spawn();
  return (await task.output()).code;
};
