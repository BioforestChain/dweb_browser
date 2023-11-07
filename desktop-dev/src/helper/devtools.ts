// 开发工具
// export class Log {
//   log(str: string) {
//     console.log(str);
//   }

//   red(str: string) {
//     console.log(`\x1B[31m%s\x1B[0m`, str);
//   }

//   green(str: string) {
//     console.log(`\x1B[32m%s\x1B[0m`, str);
//   }

//   yellow(str: string) {
//     console.log(`\x1B[33m%s\x1B[0m`, str);
//   }

//   blue(str: string) {
//     console.log(`\x1B[34m%s\x1B[0m`, str);
//   }

//   // 品红色
//   magenta(str: string) {
//     console.log(`\x1B[35m%s\x1B[0m`, str);
//   }

//   cyan(str: string) {
//     console.log(`\x1B[36m%s\x1B[0m`, str);
//   }

//   grey(str: string) {
//     console.log(`\x1B[36m%s\x1B[0m`, str);
//   }
// }
// export const log = new Log();

const standConsole = Reflect.get(globalThis, "console");
class Debugger {
  static filterList = new Set<string>();

  log(tag: string, ...args: unknown[]) {
    // if (!Debugger.filterList.has(tag)) return;
    standConsole.log(`log-   [${tag}]: `, ...args);
  }

  error(tag: string, ...args: unknown[]) {
    // if (!Debugger.filterList.has(tag)) return;
    standConsole.log(`\x1B[31m%s\x1B[0m`, `error- [${tag}]:`, ...args);
  }

  warn(tag: string, ...args: unknown[]) {
    // if (!Debugger.filterList.has(tag)) return;
    standConsole.log(`\x1B[33m%s\x1B[0m`, `warn-  [${tag}]:`, ...args);
  }

  // 始终显示
  always(tag: string, ...args: unknown[]) {
    standConsole.log(`always-[${tag}]: `, ...args);
  }
}

const console = new Debugger();
Reflect.set(globalThis, "console", console);

/**
 *
 * @param arr
 */
const setFilter = (arr: string[]) => {
  Debugger.filterList = new Set(arr);
};

export { Debugger, setFilter };

// 扩展 globalThis.Console 类型的 property
declare global {
  namespace globalThis {
    interface Console {
      always(...args: unknown[]): void; // add your method to the interface
      // error(...args: unknown[]): void;
    }
  }
}
