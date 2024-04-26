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
const customInspect = (arg: any) => {
  if (typeof arg === "object") {
    if (arg !== null && CUSTOM_INSPECT in arg) {
      return arg[CUSTOM_INSPECT]();
    } else {
      try {
        return JSON.stringify(arg);
      } catch {
        return arg;
      }
    }
  } else {
    return arg;
  }
};
const customInspects = (args: any[]) => args.filter((arg) => arg !== undefined).map(customInspect);
export const CUSTOM_INSPECT = Symbol.for("inspect.custom");
