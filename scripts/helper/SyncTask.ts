import { debounce } from "https://deno.land/std@0.182.0/async/debounce.ts";
import { copySync } from "https://deno.land/std@0.182.0/fs/copy.ts";
import { emptyDirSync } from "https://deno.land/std@0.182.0/fs/empty_dir.ts";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";
import { isDeepStrictEqual } from "node:util";

const tryFileURLToPath = (somepath: string) =>
  somepath.startsWith("file://") ? fileURLToPath(somepath) : somepath;

const relativeCwd = (to: string) => path.relative(process.cwd(), to);

export type $SyncTaskConfig = {
  to: string;
  from: string;
};
export type $SyncTaskConfigs = $SyncTaskConfig[];
export class SyncTask {
  isVerbose = Deno.args.includes("--verbose");
  constructor(readonly tasks: $SyncTaskConfigs) {}

  static from(root: $SyncTaskConfig, input_tasks: $SyncTaskConfigs) {
    const to_dir_root = tryFileURLToPath(root.to);
    const from_dir_root = tryFileURLToPath(root.from);

    const tasks = input_tasks.map((task) => {
      return {
        from: path.join(from_dir_root, task.from),
        to: path.join(to_dir_root, task.to),
      };
    });
    return new SyncTask(tasks);
  }
  static concat(...tasks: SyncTask[]) {
    return new SyncTask(tasks.map((t) => t.tasks).flat());
  }

  sync() {
    for (const task of this.tasks) {
      this._syncTask(task);
    }
  }
  private _syncTask(task: $SyncTaskConfig) {
    try {
      /// 尝试清空
      emptyDirSync(task.to);
    } catch (err) {
      console.log(err.message);
    }
    try {
      copySync(task.from, task.to, { overwrite: true });
    } catch (error) {
      if (error instanceof Deno.errors.NotFound) {
        console.warn(error.message);
      } else {
        console.error(error);
      }
    }
    console.log("synced", relativeCwd(task.from), "=>", relativeCwd(task.to));
  }

  watch(recursive = true) {
    const watcherList = this.tasks.map((task) => {
      const watcher = (() => {
        // https://deno.land/std@0.186.0/fs/mod.ts?s=existsSync
        // deno去掉了exists和existsSync方法，改用try/catch方式处理
        console.log("watching", task.from);
        Deno.mkdirSync(task.from, { recursive: true });
        return Deno.watchFs(task.from, { recursive });
      })();
      return {
        watcher,
        task,
        debounceSync: debounce(() => this._syncTask(task), 500),
      };
    });
    watcherList.forEach(async ({ watcher, debounceSync }) => {
      const statMap = new Map<string, Deno.FileInfo>();
      for await (const event of watcher) {
        if (this.isVerbose) {
          console.log(event);
        }
        /// watchFs 有bug，会被 Deno.copyFileSync 所触发，因此这里使用 stat 来手动进行变更判定
        const changedPaths = event.paths.filter((path) => {
          const preStat = statMap.get(path);
          try {
            const newStat = Deno.statSync(path);
            if (isDeepStrictEqual(newStat, preStat)) {
              return false;
            }
            statMap.set(path, newStat);
            return true;
          } catch {
            return true;
          }
        });

        if (changedPaths.length > 0) {
          debounceSync();
        }
      }
    });
  }

  auto(args = Deno.args) {
    this.sync();
    if (args.includes("--watch") || args.includes("--dev")) {
      this.watch();
    }
  }
}
