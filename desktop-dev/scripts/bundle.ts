import chalk from "https://esm.sh/v124/chalk@5.2.0";

/**
 * 所有的打包任务
 */
const bundle_tasks: Record<string, $Task> = {
    // "cot-demo":{
    //   "cmd":'pnpm',
    //   args:""  
    // },
  "jmm.html": {
    cmd: "npx",
    args: "vite build src/sys/jmm/assets/ --outDir=../../../../electron/bundle/jmm --emptyOutDir -c electron-vite.config.ts",
    devAppendArgs: "--minify=false --watch",
  },
  "js-process.worker": {
    cmd: "npx",
    args: "esbuild ./src/sys/js-process/js-process.worker.ts --outfile=./electron/bundle/js-process.worker.js --bundle --format=esm --target=es2020",
    devAppendArgs: "--watch",
  },
  "js-process.html": {
    cmd: "npx",
    args: "vite build src/sys/js-process/assets/ --outDir=../../../../electron/bundle/js-process --emptyOutDir -c electron-vite.config.ts",
    devAppendArgs: "--minify=false --watch",
  },
  public: {
    cmd: "npx",
    args: "esbuild ./src/user/public-service/public.service.worker.ts --outfile=./electron/bundle/public.service.worker.js --bundle --format=esm --target=es2020",
    devAppendArgs: "--watch",
  },
  "browser.worker": {
    cmd: "node",
    args: "./scripts/esbuild.browser.worker.js",
    devAppendArgs: "--watch",
  },
  "multi-webview.html": {
    cmd: "npx",
    args: "vite build src/sys/multi-webview/assets/ --outDir=../../../../electron/bundle/multi-webview --emptyOutDir -c electron-vite.config.ts",
    devAppendArgs: "--minify=false --watch",
  },
  "desktop.worker": {
    cmd: "npx",
    args: "esbuild ./src/user/desktop/desktop.worker.ts --outfile=./electron/bundle/desktop.worker.js --bundle --format=esm --target=es2020",
    devAppendArgs: "--watch",
  },
  "desktop_2.worker": {
    cmd: "npx",
    args: "esbuild ./src/user/desktop_2/desktop.worker.ts --outfile=./electron/bundle/desktop_2.worker.js --bundle --format=esm --target=es2020",
    devAppendArgs: "--watch",
  },
  "test.worker": {
    cmd: "npx",
    args: "esbuild ./src/user/test/test.worker.ts --outfile=./electron/bundle/test.worker.js --bundle --format=esm --target=es2020",
    devAppendArgs: "--watch",
  },
  "toy.worker": {
    cmd: "npx",
    args: "esbuild ./src/user/toy/toy.worker.ts --outfile=./electron/bundle/toy.worker.js --bundle --format=esm --target=es2020",
    devAppendArgs: "--watch",
  },
  "jmm.test.connect.worker": {
    cmd: "npx",
    args: "esbuild ./src/user/jmm-test-connect/jmmtestconnect.worker.ts --outfile=./electron/bundle/jmmtestconnect.worker.js --bundle --format=esm",
    devAppendArgs: "--watch",
  },
  "jmm.test.connect2.worker": {
    cmd: "npx",
    args: "esbuild ./src/user/jmm-test-connect2/jmmtestconnect2.worker.ts --outfile=./electron/bundle/jmmtestconnect2.worker.js --bundle --format=esm",
    devAppendArgs: "--watch",
  },
};

type $Task = {
  cmd: string;
  args: string[] | string;
  cwd?: string;
  devArgs?: string[] | string;
  devAppendArgs?: string[] | string;
};
const getArgs = (args?: string[] | string) =>
  args === undefined ? [] : Array.isArray(args) ? args : args.split(/\s+/);

const filters = (Deno.args.filter(arg=>!arg.startsWith("-"))[0] || "*")
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

for (const name in bundle_tasks) {
  if (filters.some((f) => f(name))) {
    const task = bundle_tasks[name];
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
  }
}
