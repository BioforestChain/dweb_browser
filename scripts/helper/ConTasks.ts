import { whichSync } from "jsr:@david/which";
import node_path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";
import { format } from "node:util";
import picocolors from "npm:picocolors";
import { PromiseOut } from "../../toolkit/dweb-helper/src/PromiseOut.ts";
import { mapHelper } from "../../toolkit/dweb-helper/src/fun/mapHelper.ts";
import type { DenoOS } from "../deps.ts";

export const ExitAbortController = new AbortController();
export type $Tasks = Record<string, $Task>;
export type $Task = {
  cmd: string;
  args: string[] | string;
  os?: DenoOS.OSType;
  cwd?: string;
  signal?: AbortSignal;
  devArgs?: string[] | string;
  devAppendArgs?: string[] | string;
  /** 启动依赖项 */
  startDeps?: $StartDep[];
  env?: Record<string, string>;
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
    logLineFilter: $LogLineFilter = (_line) => true
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
    const waitter = mapHelper.getOrPut(this._waitters, fragment, () => new PromiseOut());
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
      task.cwd = node_path.resolve(base, task.cwd ?? "./");
      task.signal = ExitAbortController.signal;
    }
  }
  #task_args_map = new WeakMap<$Task, string[]>();
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
      if (filters.some((f) => f(name)) === false) {
        continue;
      }
      const task = this.tasks[name];
      const args = useDev
        ? task.devArgs
          ? getArgs(task.devArgs)
          : [...getArgs(task.args), ...getArgs(task.devAppendArgs)]
        : getArgs(task.args);

      if (task.cmd === "npx") {
        args.unshift("--yes"); /// 避免需要确认安装的交互
      }

      // 修复windows无法找到命令执行环境问题
      const cmd = whichSync(task.cmd);
      this.#task_args_map.set(task, args);

      const command = new Deno.Command(cmd!, {
        args: args,
        cwd: task.cwd,
        stderr: "piped",
        stdout: "piped",
        stdin: "piped",
        env: task.env,
      });
      this;
      children[name] = {
        command,
        task,
        stdoutLogger: new TaskLogger(picocolors.blue(name + " "), Deno.stdout, task.logTransformer, task.logLineFilter),
        stderrLogger: new TaskLogger(picocolors.red(name + " "), Deno.stderr, task.logTransformer, task.logLineFilter),
      };
    }
    /// 根据依赖顺序，启动任务
    const processTasks: Promise<void>[] = [];
    /// 打印要执行的任务
    {
      const childrenNames = new Set(Object.keys(children));
      console.log(picocolors.blue("---Tasks:---"));
      Object.keys(this.tasks).forEach((taskname, index) => {
        const enable = childrenNames.has(taskname);
        console.log(
          picocolors.blue(`${index}.`.padStart(3, " ")),
          enable ? picocolors.green("☑") : picocolors.gray("☐"),
          enable ? picocolors.green(taskname) : picocolors.gray(taskname)
        );
      });
      console.log(picocolors.blue("------------"));
    }
    for (const name in children) {
      const { task, command, stdoutLogger, stderrLogger } = children[name];
      const processTask = (async () => {
        /// 等待依赖执行完成
        if (task.startDeps?.length) {
          const allWhenLogs = task.startDeps
            .map((dep) => {
              console.log(picocolors.gray(name + " "), `waitting dep: ${dep.name} log: ${dep.whenLog}`);
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
        console.log(picocolors.magenta(picocolors.bold(name)), "", picocolors.cyan("---- begin ----"));
        console.log(
          picocolors.green(">"),
          picocolors.magenta(picocolors.bold("cd")),
          picocolors.magenta(task.cwd ?? Deno.cwd())
        );
        console.log(
          picocolors.green(">"),
          picocolors.magenta(picocolors.bold(task.cmd)),
          picocolors.magenta(this.#task_args_map.get(task)?.join(" ") ?? "")
        );
        // 生成子进程
        const child = command.spawn();
        const listener = () => {
          try {
            child.kill();
            // deno-lint-ignore no-empty
          } catch (_) {}
        };
        task.signal?.addEventListener("abort", listener);
        child.stdout.pipeThrough(new TextDecoderStream()).pipeTo(stdoutLogger);
        await child.stderr.pipeThrough(new TextDecoderStream()).pipeTo(stderrLogger);

        console.log(picocolors.bold(picocolors.magenta(name)), "", picocolors.cyan("---- done ----"));
        task.signal?.removeEventListener("abort", listener);
      })().catch((err) => {
        console.error(
          format(picocolors.bold(`${name}`), err)
            .split("\n")
            .map((line, index) => {
              if (index === 0) {
                return picocolors.red(line);
              }
              if (/at (.+) \(/.test(line)) {
                line = line.replace(
                  /at (.+) \((.+?)\)/g,
                  (_, name, file) => `at ${picocolors.bold(picocolors.italic(name))} (${picocolors.cyan(file)})`
                );
              } else {
                line = line.replace(/at (.+)/g, (_, file) => `at ${picocolors.cyan(file)}`);
              }
              return picocolors.red(line.replace(/\:(\d+)/g, (_, num) => `:${picocolors.yellow(num)}`));
            })
            .join("\n")
        );
        process.exit(1);
      });
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

      if (task.os && task.os !== Deno.build.os) {
        continue;
      }

      this.tasks[newTaskName] = task;
    }
    return this;
  }
}
