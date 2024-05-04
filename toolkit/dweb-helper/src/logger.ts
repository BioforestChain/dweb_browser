import { once } from "./decorator/$once.ts";

const tabify = (str: string, tabSize = 4) => str.padEnd(Math.ceil(str.length / tabSize) * tabSize, " ");
export class Logger {
  constructor(readonly scope: unknown) {}
  isEnable = true;
  /**
   * 这里需要延迟获取，否则 scope 的 toString 相关的参数可能在构造函数阶段，还没完成
   */
  @once()
  get prefix() {
    return tabify(String(this.scope)) + "|";
  }
  log = (tag: string, ...args: unknown[]) => {
    if (this.isEnable) console.log(this.prefix, tabify(tag) + "|", ...customInspects(args));
  };
  verbose = (tag: string, ...args: unknown[]) => {
    if (this.isEnable) console.debug(this.prefix, tabify(tag) + "|", ...customInspects(args));
  };
  error = (tag: string, ...args: unknown[]) => {
    if (this.isEnable) console.error(this.prefix, tabify(tag) + "|", ...customInspects(args));
  };
  warn = (...args: unknown[]) => {
    return console.warn(this.prefix, ...customInspects(args));
  };
  debugLazy = (tag: string, lazy: () => any) => {
    if (this.isEnable) {
      const args = lazy();
      if (Symbol.iterator in args) {
        console.debug(this.prefix, tabify(tag) + "|", ...customInspects(args));
      } else {
        console.debug(this.prefix, tabify(tag) + "|", customInspect(args));
      }
    }
  };
  errorLazy = (tag: string, lazy: () => any) => {
    if (this.isEnable) {
      const args = lazy();
      if (Symbol.iterator in args) {
        console.error(this.prefix, tabify(tag) + "|", ...customInspects(args));
      } else {
        console.error(this.prefix, tabify(tag) + "|", customInspect(args));
      }
    }
  };
}

export const logger = (scope: unknown) => {
  return new Logger(scope);
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
