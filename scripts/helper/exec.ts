import { whichSync } from "jsr:@david/which";
import picocolors from "npm:picocolors";
import { createBaseResolveTo } from "./resolveTo.ts";
import { WalkAny } from "./WalkDir.ts";

let defaultResolveTo = createBaseResolveTo(Deno.cwd());
let preCwd = Deno.cwd();
export type CmdOptions = {
  cwd?: string | URL;
  silent?: boolean;
  useWhich?: boolean;
  onSpawn?: (childProcess: Deno.ChildProcess) => void;
  onStdout?: (log: string) => string | void;
  onStderr?: (log: string) => string | void;
};
export const $ = Object.assign(
  (cmd: string | string[], options: CmdOptions = {}) => {
    const { useWhich = false, onSpawn, onStdout, onStderr, silent = false } = options;
    if (typeof cmd === "string") {
      cmd = cmd.split(/\s+/);
    }
    const [exec, ...args] = cmd;
    const cmdWhich = useWhich ? (exec.startsWith("./") ? exec : whichSync(exec)) : exec;
    const cwd = defaultResolveTo(options.cwd ?? "./");
    if (preCwd !== cwd) {
      preCwd = cwd;
      silent || console.log(picocolors.green(">"), picocolors.magenta(picocolors.bold("cd")), picocolors.magenta(cwd));
    }
    silent ||
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
          if (outline !== "") {
            Deno.stdout.writeSync(encoder.encode(outline));
          }
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
    string: (cmd: string | string[], options?: CmdOptions) => {
      return new Promise<string>((resolve, reject) => {
        let res = "";
        $(cmd, {
          ...options,
          onStdout: (chunk) => {
            res += chunk;
            if (options?.onStdout) {
              return options.onStdout(chunk);
            }
            return "";
          },
        }).then((status) => {
          if (status.success) {
            resolve(res);
          } else {
            reject(`(${status.code}) ${status.signal}`);
          }
        }, reject);
      });
    },
  }
);
