import picocolors from "npm:picocolors";
import { WalkAny } from "./WalkDir.ts";
import { whichSync } from "./WhichCommand.ts";
import { createBaseResolveTo } from "./resolveTo.ts";

let defaultResolveTo = createBaseResolveTo(Deno.cwd());
let preCwd = Deno.cwd();
export const $ = Object.assign(
  async (
    cmd: string | string[],
    cwd?: string | URL,
    options: {
      useWhich?: boolean;
      onSpawn?: (childProcess: Deno.ChildProcess) => void;
      onStdout?: (log: string) => string | void;
      onStderr?: (log: string) => string | void;
    } = {}
  ) => {
    const { useWhich = false, onSpawn, onStdout, onStderr } = options;
    if (typeof cmd === "string") {
      cmd = cmd.split(/\s+/);
    }
    const [exec, ...args] = cmd;
    const cmdWhich = useWhich ? (exec.startsWith("./") ? exec : whichSync(exec)) : exec;
    cwd = defaultResolveTo(cwd ?? "./");
    if (preCwd !== cwd) {
      preCwd = cwd;
      console.log(picocolors.green(">"), picocolors.magenta(picocolors.bold("cd")), picocolors.magenta(cwd));
    }
    console.log(picocolors.green(">"), picocolors.magenta(picocolors.bold(exec)), picocolors.magenta(args.join(" ")));
    const command = new Deno.Command(cmdWhich!, {
      args,
      cwd,
      stdout: onStdout ? "piped" : "inherit",
      stderr: onStderr ? "piped" : "inherit",
      env: Deno.env.toObject(),
    });

    const childProcess = command.spawn();
    onSpawn?.(childProcess);
    const encoder = new TextEncoder();
    if (onStdout) {
      (async () => {
        const decoder = new TextDecoderStream();
        for await (const line of childProcess.stdout.pipeThrough(decoder)) {
          const outline = onStdout(line) ?? line;
          Deno.stdout.writeSync(encoder.encode(outline));
        }
      })();
    }

    if (onStderr) {
      (async () => {
        const decoder = new TextDecoderStream();
        for await (const line of childProcess.stderr.pipeThrough(decoder)) {
          const outline = onStderr(line) ?? line;
          Deno.stdout.writeSync(encoder.encode(outline));
        }
      })();
    }

    return childProcess.status;
  },
  {
    cd: (dir: string | URL) => {
      defaultResolveTo = createBaseResolveTo(defaultResolveTo(dir));
    },
    pwd: () => defaultResolveTo(),
    ls: (dir: string | URL) => WalkAny(defaultResolveTo(dir)),
  }
);
