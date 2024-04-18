const tabify = (str: string, tabSize = 4) => str.padEnd(Math.ceil(str.length / tabSize) * tabSize, " ");
export const logger = (scope: unknown) => {
  const prefix = tabify(String(scope)) + "|";
  const logger = {
    isEnable: true,
    debug: (tag: string, ...args: unknown[]) => {
      if (logger.isEnable) console.debug(prefix, tabify(tag) + "|", ...args);
    },
    error: (tag: string, ...args: unknown[]) => {
      if (logger.isEnable) console.error(prefix, tabify(tag) + "|", ...args);
    },
    warn: (...args: unknown[]) => {
      return console.warn(prefix, ...args);
    },
    debugLazy: (tag: string, lazy: () => any) => {
      if (logger.isEnable) {
        let args = lazy();
        if (Symbol.iterator in args) {
          console.debug(prefix, tabify(tag) + "|", ...args);
        } else {
          console.debug(prefix, tabify(tag) + "|", args);
        }
      }
    },
    errorLazy: (tag: string, lazy: () => any) => {
      if (logger.isEnable) {
        let args = lazy();
        if (Symbol.iterator in args) {
          console.error(prefix, tabify(tag) + "|", ...args);
        } else {
          console.error(prefix, tabify(tag) + "|", args);
        }
      }
    },
  };
  return logger;
};
