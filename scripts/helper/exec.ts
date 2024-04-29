import { createBaseResolveTo } from "./ConTasks.helper.ts";
import { WalkAny } from "./WalkDir.ts";
import { whichSync } from "./WhichCommand.ts";

let defaultResolveTo = createBaseResolveTo(Deno.cwd());
export const $ = Object.assign(
  async (cmd: string | string[], cwd?: string | URL) => {
    if (typeof cmd === "string") {
      cmd = cmd.split(/\s+/);
    }
    const [exec, ...args] = cmd;
    const cmdWhich = exec.startsWith("./") ? exec : whichSync(exec);
    const command = new Deno.Command(cmdWhich!, { args, cwd: defaultResolveTo(cwd ?? "./"), stdout: "inherit" });
    await command.output();
  },
  {
    cd: (dir: string | URL) => {
      defaultResolveTo = createBaseResolveTo(defaultResolveTo(dir));
    },
    pwd: () => defaultResolveTo(),
    ls: (dir: string | URL) => WalkAny(defaultResolveTo(dir)),
  }
);
