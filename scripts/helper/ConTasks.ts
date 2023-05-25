import chalk from "https://esm.sh/v124/chalk@5.2.0";

export type $Task = {
  cmd: string;
  args: string[] | string;
  cwd?: string;
  devArgs?: string[] | string;
  devAppendArgs?: string[] | string;
};
export type $Tasks = Record<string, $Task>;
const getArgs = (args?: string[] | string) =>
  args === undefined ? [] : Array.isArray(args) ? args : args.split(/\s+/);

const filters = (Deno.args.filter((arg) => !arg.startsWith("-"))[0] || "*")
  .trim()
  .split(/\s*,\s*/)
  .map((f) => {
    if (f.includes("*")) {
      const reg = new RegExp(f.replace(/\*/g, ".*"));
      return (name: string) => reg.test(name);
    }
    return (name: string) => name === f;
  });

const useDev = Deno.args.includes("--dev");

class Logger extends WritableStream<string> {
  constructor(prefix: string) {
    super({
      write: async (chunk) => {
        let log = chunk
          .split(/\n/g)
          .map((line) => {
            if (line.length) {
              return prefix + line;
            }
            return line;
          })
          .join("\n");
        await Logger.stdWrite(this, log);
      },
    });
  }
  static textEncoder = new TextEncoder();
  static stdWrite(from: Logger, content: string) {
    return Deno.stdout.write(this.textEncoder.encode(content));
  }
}

/**
 * 并发执行任务
 */
export class ConTasks {
  constructor(readonly tasks: $Tasks) {}
  spawn() {
    const children: Deno.ChildProcess[] = [];
    for (const name in this.tasks) {
      if (filters.some((f) => f(name))) {
        const task = this.tasks[name];
        const args = useDev
          ? task.devArgs
            ? getArgs(task.devArgs)
            : [...getArgs(task.args), ...getArgs(task.devAppendArgs)]
          : getArgs(task.args);
        if (task.cmd === "npx") {
          args.unshift("--yes");
        }
        const command = new Deno.Command(task.cmd, {
          args: args,
          cwd: task.cwd,
          stderr: "piped",
          stdout: "piped",
        });
        const child = command.spawn();
        child.stdout
          .pipeThrough(new TextDecoderStream())
          .pipeTo(new Logger(chalk.blue(name + " ")));
        child.stderr
          .pipeThrough(new TextDecoderStream())
          .pipeTo(new Logger(chalk.red(name + " ")));
        children.push(child);
      }
    }
    return children;
  }
}
