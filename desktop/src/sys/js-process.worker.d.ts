/// <reference lib="webworker"/>

declare var process: Awaited<
  ReturnType<typeof import("./js-process.worker.cjs")["installEnv"]>
>;
