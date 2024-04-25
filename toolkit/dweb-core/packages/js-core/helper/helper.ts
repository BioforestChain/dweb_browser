import { simpleEncoder } from "./encoding.ts";

// deno-lint-ignore no-explicit-any
type Func<T extends any[], R> = (...args: T) => R;

// deno-lint-ignore no-explicit-any
export function once<T extends any[], R>(func: Func<T, R>): Func<T, R> {
  let result: R | undefined;
  let called = false;

  return function (...args: T): R {
    if (!called) {
      result = func(...args);
      called = true;
    }
    return result as R;
  };
}

/**
 * 查找子数组对象
 * @param arr
 * @param subArr
 * @returns
 */
export function injectSubArray(arr: Uint8Array, subList: number[]) {
  const arrList = Array.from(arr);
  for (let i = 0; i <= arrList.length - subList.length; i++) {
    if (arrList.slice(i, i + subList.length).toString() === subList.toString()) {
      // 找到目标子数组，使用splice方法插入新数组
      const insertList = simpleEncoder(`\n    <meta is="dweb-config"/>`, "utf8");
      arr.set(insertList, i + subList.length);
      return arr;
    }
  }
  return arr;
}
