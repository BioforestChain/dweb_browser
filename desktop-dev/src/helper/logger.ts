const tabify = (str: string, tabSize = 4) => str.padEnd(Math.ceil(str.length / tabSize) * tabSize, " ");
export const logger = (scope: unknown) => {
  const prefix = tabify(String(scope)) + "|";
  const logger = {
    isEnable: true,
    debug: (tag: string, ...args: unknown[]) => {
      if (logger.isEnable) console.debug(prefix, tabify(tag) + "|", ...customInspects(args));
    },
    error: (tag: string, ...args: unknown[]) => {
      if (logger.isEnable) console.error(prefix, tabify(tag) + "|", ...customInspects(args));
    },
    warn: (...args: unknown[]) => {
      return console.warn(prefix, ...customInspects(args));
    },
    debugLazy: (tag: string, lazy: () => any) => {
      if (logger.isEnable) {
        let args = lazy();
        if (Symbol.iterator in args) {
          console.debug(prefix, tabify(tag) + "|", ...customInspects(args));
        } else {
          console.debug(prefix, tabify(tag) + "|", customInspect(args));
        }
      }
    },
    errorLazy: (tag: string, lazy: () => any) => {
      if (logger.isEnable) {
        let args = lazy();
        if (Symbol.iterator in args) {
          console.error(prefix, tabify(tag) + "|", ...customInspects(args));
        } else {
          console.error(prefix, tabify(tag) + "|", customInspect(args));
        }
      }
    },
  };
  return logger;
};
const customInspect = (arg: any) =>
  typeof arg === "object" && arg !== null && CUSTOM_INSPECT in arg ? arg[CUSTOM_INSPECT]() : arg;
const customInspects = (args: any[]) => args.map(customInspect);
export const CUSTOM_INSPECT = Symbol.for("inspect.custom");
