import path from "node:path";
import { fileURLToPath } from "node:url";
export const __dirname = fileURLToPath(import.meta.resolve("./"));
console.log("__dirname:", __dirname);
export const sourceCodeDir = fileURLToPath(import.meta.resolve("../../../../app/iosApp/"));
console.log("sourceCodeDir:", sourceCodeDir);

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
  console.log(`exec ${commands.join(" ")}\n\tcwd: ${cwd}`);
  const task = new Deno.Command(command, {
    cwd,
    args,
    stdin: "inherit",
    stdout: "inherit",
  }).spawn();
  return (await task.output()).code;
};
