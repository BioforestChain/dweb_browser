/**
 * 一个防抖函数，该函数将在给定的 `wait` 毫秒内被延迟执行。
 * 如果在超时到期之前再次调用该方法，则会中止上一次调用。
 */
export interface DebouncedFunction<T extends Array<unknown>> {
  (...args: T): void;
  /** 清除防抖的超时时间，并取消调用防抖函数。 */
  clear(): void;
  /** 清除防抖的超时时间，并立即调用防抖函数。 */
  flush(): void;
  /** 返回一个布尔值，指示防抖调用是否处于挂起状态。 */
  readonly pending: boolean;
}

/**
 * 创建一个防抖函数，该函数将延迟给定的 `func` 在指定的 `wait` 毫秒内的执行。
 * 如果在超时到期之前再次调用该方法，则会中止上一次调用。
 *
 * @example 使用示例
 * ```js
 * import { debounce } from './debounce'; // 替换为实际的路径
 *
 * const debouncedFn = debounce((event) => {
 *   console.log('[%s] %s', event.kind, event.paths[0]);
 * }, 200);
 *
 * const watcher = fs.watch('./', () => debouncedFn(event));
 * // 等待 200ms ...
 * // 输出: 函数在 200ms 后防抖调用
 * ```
 *
 * @typeParam T 提供函数的参数类型。
 * @param fn 需要防抖的函数。
 * @param wait 延迟函数执行的时间（毫秒）。
 * @returns 防抖处理后的函数。
 */

// deno-lint-ignore no-explicit-any
export function debounce<T extends Array<any>>(
  fn: (this: DebouncedFunction<T>, ...args: T) => void,
  wait: number
): DebouncedFunction<T> {
  // deno-lint-ignore no-explicit-any
  let timeout: any | null = null; // Node.js 中 setTimeout 返回Timeout类型
  let flush: (() => void) | null = null;

  const debounced: DebouncedFunction<T> = ((...args: T) => {
    debounced.clear();
    flush = () => {
      debounced.clear();
      fn.call(debounced, ...args);
    };
    timeout = setTimeout(flush, wait);
  }) as DebouncedFunction<T>;

  debounced.clear = () => {
    if (timeout) {
      clearTimeout(timeout);
      timeout = null;
      flush = null;
    }
  };

  debounced.flush = () => {
    flush?.();
  };

  Object.defineProperty(debounced, "pending", {
    get: () => timeout !== null,
  });

  return debounced;
}
