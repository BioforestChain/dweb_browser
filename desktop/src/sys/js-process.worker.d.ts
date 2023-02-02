/// <reference lib="webworker"/>

declare var process: ReturnType<
  typeof import("./js-process.worker.cjs")["installEnv"]
>;
