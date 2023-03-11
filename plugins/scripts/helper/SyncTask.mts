import path from "node:path";
import { fileURLToPath } from "node:url";
import { debounce } from "https://deno.land/std@0.109.0/async/debounce.ts";
import { copySync } from "https://deno.land/std@0.179.0/fs/copy.ts";
import { ensureDirSync } from "https://deno.land/std@0.179.0/fs/ensure_dir.ts";
import { emptyDirSync } from "https://deno.land/std@0.179.0/fs/mod.ts";

const tryFileURLToPath = (somepath: string) =>
  somepath.startsWith("file://") ? fileURLToPath(somepath) : somepath;

export class SyncTask {
  constructor(
    root: {
      to: string;
      from: string;
    },
    input_tasks: Array<{ from: string; to: string }>
  ) {
    const to_dir_root = tryFileURLToPath(root.to);
    const from_dir_root = tryFileURLToPath(root.from);

    this.tasks = input_tasks.map((task) => {
      return {
        from: path.join(from_dir_root, task.from),
        to: path.join(to_dir_root, task.to),
      };
    });
  }

  tasks: Array<{ from: string; to: string }>;

  sync() {
    for (const task of this.tasks) {
      ensureDirSync(task.to);
      emptyDirSync(task.to);
      try {
        copySync(task.from, task.to, { overwrite: true });
      } catch (error) {
        if (error instanceof Deno.errors.NotFound) {
          console.warn(error.message);
        } else {
          throw error;
        }
      }
    }
    console.log("synced");
  }

  watch() {
    const debounceSync = debounce(() => this.sync(), 500);
    const watcherList = this.tasks.map((task) =>
      Deno.watchFs(task.from, { recursive: true })
    );
    console.log("watching");
    watcherList.forEach(async (watcher) => {
      for await (const event of watcher) {
        console.log(event)
        debounceSync();
      }
    });
  }

  auto(args = Deno.args) {
    this.sync();
    if (args.includes("--watch")) {
      this.watch();
    }
  }
}
