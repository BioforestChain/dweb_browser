import { iter_first_not_null, map_get_or_put } from "@gaubee/util";
import { once } from "./decorator/$once.ts";

export class DebugTags {
  constructor(
    readonly debugRegexes: Set<RegExp>,
    readonly debugNames: Set<string>,
    readonly verboseRegexes: Set<RegExp>,
    readonly verboseNames: Set<string>
  ) {}
  static get singleton() {
    return this._singleton;
  }
  private static _singleton = new DebugTags(new Set(), new Set(), new Set(), new Set());
  static from(tags: Iterable<string>) {
    const debugRegexes = new Set<RegExp>();
    const debugNames = new Set<string>();
    const verboseRegexes = new Set<RegExp>();
    const verboseNames = new Set<string>();
    const anyRegList = [debugRegexes, verboseRegexes];
    const anyNamList = [debugNames, verboseNames];
    const debRegList = [debugRegexes];
    const debNamList = [debugNames];
    const verRegList = [verboseRegexes];
    const verNamList = [verboseNames];
    for (const _tag of tags) {
      const tag = _tag.trim();
      if (tag.length === 0) {
        continue;
      }
      let regList = debRegList;
      let namList = anyNamList;
      let safeTag = tag;
      if (tag.startsWith(":debug:")) {
        safeTag = tag.slice(":debug:".length);
        regList = debRegList;
        namList = debNamList;
      } else if (tag.startsWith(":verbose:")) {
        safeTag = tag.slice(":verbose:".length);
        regList = verRegList;
        namList = verNamList;
      } else if (tag.startsWith(":all:")) {
        safeTag = tag.slice(":all:".length);
        regList = anyRegList;
        namList = anyNamList;
      }

      if (safeTag.startsWith("/") && safeTag.endsWith("/")) {
        const reg = new RegExp(safeTag.slice(1, -1));
        for (const regexes of regList) {
          regexes.add(reg);
        }
      } else {
        for (const names of namList) {
          names.add(safeTag);
        }
      }
    }
    this._singleton = new DebugTags(debugRegexes, debugNames, verboseRegexes, verboseNames);
  }

  private readonly debugResult = new Map<string, boolean>();
  canDebug(scope: string) {
    return map_get_or_put(this.debugResult, scope, () => {
      return (
        this.debugNames.has(scope) ||
        iter_first_not_null(this.debugRegexes, (regex: RegExp) => {
          return scope.match(regex);
        }) != null
      );
    });
  }

  private readonly verboseResult = new Map<string, boolean>();
  canVerbose(scope: string) {
    return map_get_or_put(this.verboseResult, scope, () => {
      return (
        this.verboseNames.has(scope) ||
        iter_first_not_null(this.verboseRegexes, (regex: RegExp) => {
          return scope.match(regex);
        }) != null
      );
    });
  }
  canTimeout(scope: string) {
    return this.canDebug(scope);
  }
}

export const addDebugTags = (tags: Iterable<string>) => {
  DebugTags.from(tags);
};

const tabify = (str: string, tabSize = 4) => str.padEnd(Math.ceil(str.length / tabSize) * tabSize, " ");
export class Logger {
  constructor(scope: unknown) {
    this.scope = String(scope);
  }
  readonly scope: string;
  private _isEnable = true;
  get isEnable() {
    this.syncDebugTags();
    return this._isEnable;
  }
  private _isEnableVerbose = true;
  get isEnableVerbose() {
    this.syncDebugTags();
    return this._isEnableVerbose;
  }
  private _isEnableTimeout = true;
  get isEnableTimeout() {
    this.syncDebugTags();
    return this._isEnableTimeout;
  }
  private syncDebugTags() {
    this.debugTags = DebugTags.singleton;
  }

  private _debugTags: DebugTags | null = null;
  private get debugTags() {
    return this._debugTags;
  }
  private set debugTags(value: DebugTags | null) {
    if (value !== this._debugTags) {
      if (value == null) {
        this._isEnableVerbose = false;
        this._isEnableTimeout = false;
        this._isEnable = false;
      } else {
        this._isEnableVerbose = value.canVerbose(this.scope);
        this._isEnableTimeout = value.canTimeout(this.scope);
        this._isEnable = value.canDebug(this.scope);
      }
      this._debugTags = value;
    }
  }
  /**
   * 这里需要延迟获取，否则 scope 的 toString 相关的参数可能在构造函数阶段，还没完成
   */
  @once()
  get prefix() {
    return tabify(this.scope) + "|";
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
  // deno-lint-ignore no-explicit-any
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
  // deno-lint-ignore no-explicit-any
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
// deno-lint-ignore no-explicit-any
export const customInspect = (arg: any) => {
  if (typeof arg !== "object") {
    return arg;
  }
  if (arg !== null && CUSTOM_INSPECT in arg) {
    arg = arg[CUSTOM_INSPECT]();
  }
  if (typeof arg === "object" && arg !== null) {
    try {
      return JSON.stringify(arg, (_key, value) => {
        if (typeof value === "object" && value !== null && CUSTOM_INSPECT in value) {
          return value[CUSTOM_INSPECT]();
        }
        return value;
      });
    } catch {
      return arg;
    }
  }
  return arg;
};
// deno-lint-ignore no-explicit-any
const customInspects = (args: any[]) => args.filter((arg) => arg !== undefined).map(customInspect);
export const CUSTOM_INSPECT = Symbol.for("inspect.custom") as unknown as "inspect.custom";
