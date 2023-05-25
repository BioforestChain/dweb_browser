import { debounce } from "https://deno.land/std@0.109.0/async/debounce.ts";
import { copySync } from "https://deno.land/std@0.179.0/fs/copy.ts";
import { emptyDirSync } from "https://deno.land/std@0.179.0/fs/mod.ts";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

const tryFileURLToPath = (somepath: string) =>
  somepath.startsWith("file://") ? fileURLToPath(somepath) : somepath;

const relativeCwd = (to: string) => path.relative(process.cwd(), to);

export class SyncTask {
  isVerbose = Deno.args.includes("--verbose");
  constructor(readonly tasks: Array<{ from: string; to: string }>) {}

  static from(
    root: {
      to: string;
      from: string;
    },
    input_tasks: Array<{ from: string; to: string }>
  ) {
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
      console.log(
        "syncing",
        relativeCwd(task.from),
        "=>",
        relativeCwd(task.to)
      );
      try {
        /// 尝试清空
        emptyDirSync(task.to);
      } catch (err) {
        console.warn(err);
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
    }
    console.log("synced");
  }

  watch(recursive = false) {
    const debounceSync = debounce(() => this.sync(), 500);
    const watcherList = this.tasks.map((task) => {
      // https://deno.land/std@0.186.0/fs/mod.ts?s=existsSync
      // deno去掉了exists和existsSync方法，改用try/catch方式处理
      try {
        return Deno.watchFs(task.from, { recursive });
      } catch (error) {
        if (error instanceof Deno.errors.NotFound) {
          Deno.mkdirSync(task.from, { recursive: true });
          return Deno.watchFs(task.from, { recursive });
        }
        throw error;
      }
    });
    console.log("watching");
    watcherList.forEach(async (watcher) => {
      for await (const event of watcher) {
        if (this.isVerbose) {
          console.log(event.kind, event.paths);
        }
        debounceSync();
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
