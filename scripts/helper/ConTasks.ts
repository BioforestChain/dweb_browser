import chalk from "https://esm.sh/v124/chalk@5.2.0";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { PromiseOut } from "../../desktop-dev/src/helper/PromiseOut.ts";
import { mapHelper } from "../../desktop-dev/src/helper/mapHelper.ts";
import { whichSync } from "./WhichCommand.ts";

export type $Tasks = Record<string, $Task>;
export type $Task = {
  cmd: string;
  args: string[] | string;
  cwd?: string;
  devArgs?: string[] | string;
  devAppendArgs?: string[] | string;
  /** 启动依赖项 */
  startDeps?: $StartDep[];
  logTransformer?: $LogTransformer;
  logLineFilter?: $LogLineFilter;
};
export type $StartDep = {
  name: string;
  whenLog: string;
  logType?: "stdout" | "stderr" | "any";
};
export type $LogTransformer = (log: string) => string;
export type $LogLineFilter = (line: string) => boolean;
const getArgs = (args?: string[] | string) =>
  args === undefined ? [] : Array.isArray(args) ? args : args.split(/\s+/);

class TaskLogger extends WritableStream<string> {
  constructor(
    prefix: string,
    readonly writter: Deno.Writer,
    logTransformer: $LogTransformer = (log) => log,
    logLineFilter: $LogLineFilter = (line) => true
  ) {
    super({
      write: async (chunk) => {
        chunk = logTransformer(chunk);
        /// 如果有等待任务，那么进行判定
        if (this._waitters.size > 0) {
          const chunkText = chunk.replace(
            // deno-lint-ignore no-control-regex
            /[\u001b\u009b][[()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-ORZcf-nqry=><]/g,
            ""
          );
          for (const fragment of this._waitters.keys()) {
            if (chunkText.includes(fragment)) {
              const waitter = mapHelper.getAndRemove(this._waitters, fragment)!;
              waitter.resolve();
            }
          }
        }

        /// 加上前缀
        const log = chunk
          .split(/\n/g)
          .filter(logLineFilter)
          .map((line) => {
            if (line.length) {
              return prefix + line;
            }
            return line;
          })
          .join("\n");
        await this.write(log);
      },
    });
  }
  static textEncoder = new TextEncoder();
  write(content: string) {
    return this.writter.write(TaskLogger.textEncoder.encode(content));
  }
  private _waitters = new Map<string, PromiseOut<void>>();
  waitContent(fragment: string) {
    const waitter = mapHelper.getOrPut(
      this._waitters,
      fragment,
      () => new PromiseOut()
    );
    return waitter.promise;
  }
}

/**
 * 并发执行任务
 */
export class ConTasks {
  constructor(readonly tasks: $Tasks, base: string) {
    if (base.startsWith("file:")) {
      base = fileURLToPath(base);
    }
    for (const task of Object.values(tasks)) {
      task.cwd = path.resolve(base, task.cwd ?? "./");
    }
  }
  spawn(args = Deno.args) {
    const filters = (args.filter((arg) => !arg.startsWith("-"))[0] || "*")
      .trim()
      .split(/\s*,\s*/)
      .map((f) => {
        if (f.includes("*")) {
          const reg = new RegExp(f.replace(/\*/g, ".*"));
          return (name: string) => reg.test(name);
        }
        return (name: string) => name.startsWith(f);
      });

    const useDev = args.includes("--dev");

    const children: Record<
      string,
      {
        task: $Task;
        command: Deno.Command;
        stdoutLogger: TaskLogger;
        stderrLogger: TaskLogger;
      }
    > = {};
    /// 先便利构建出所有任务
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

        // 修复windows无法找到命令执行环境问题
        const cmd = whichSync(task.cmd);
        const command = new Deno.Command(cmd!, {
          args: args,
          cwd: task.cwd,
          stderr: "piped",
          stdout: "piped",
        });
        children[name] = {
          command,
          task,
          stdoutLogger: new TaskLogger(
            chalk.blue(name + " "),
            Deno.stdout,
            task.logTransformer,
            task.logLineFilter
          ),
          stderrLogger: new TaskLogger(
            chalk.red(name + " "),
            Deno.stderr,
            task.logTransformer,
            task.logLineFilter
          ),
        };
      }
    }
    /// 根据依赖顺序，启动任务
    const processTasks: Promise<void>[] = [];
    console.log(Object.keys(this.tasks), "=>", Object.keys(children));
    for (const name in children) {
      const { task, command, stdoutLogger, stderrLogger } = children[name];
      const processTask = (async () => {
        /// 等待依赖执行完成
        if (task.startDeps?.length) {
          const allWhenLogs = task.startDeps
            .map((dep) => {
              console.log(
                chalk.gray(name + " "),
                `waitting dep: ${dep.name} log: ${dep.whenLog}`
              );
              const child = children[dep.name];
              if (child === undefined) {
                throw new Error(`no found start-dep-task: ${dep.name}`);
              }
              const whenLogs: Promise<void>[] = [];
              const { logType = "any" } = dep;
              if (logType === "any" || logType === "stdout") {
                whenLogs.push(child.stdoutLogger.waitContent(dep.whenLog));
              }
              if (logType === "any" || logType === "stderr") {
                whenLogs.push(child.stderrLogger.waitContent(dep.whenLog));
              }
              return whenLogs;
            })
            .flat();
          await Promise.all(allWhenLogs);
        }
        /// 开始启动任务
        console.log(chalk.gray(name + " "), chalk.cyan("---- begin ----"));

        const child = command.spawn();
        child.stdout.pipeThrough(new TextDecoderStream()).pipeTo(stdoutLogger);
        await child.stderr
          .pipeThrough(new TextDecoderStream())
          .pipeTo(stderrLogger);

        console.log(chalk.gray(name + " "), chalk.cyan("---- done ----"));
      })();
      processTasks.push(processTask);
    }
    return {
      children,
      processTasks,
      afterComplete: () => Promise.all(processTasks),
    };
  }
  merge(comTasks: ConTasks, prefix = "") {
    for (const [name, task] of Object.entries(comTasks.tasks)) {
      const newTaskName = prefix + name;
      if (this.tasks[newTaskName]) {
        throw new Error(`Duplicate task name: ${newTaskName}`);
      }
      this.tasks[newTaskName] = task;
    }
    return this;
  }
}
