// deno-lint-ignore ban-types
export const once = <T extends Function>(fn: T) => {
  let first = true;
  let resolved: any;
  let rejected: any;
  let success = false;
  return function (this: any, ...args: unknown[]) {
    if (first) {
      first = false;
      try {
        resolved = fn.apply(this, args);
        success = true;
      } catch (err) {
        rejected = err;
      }
    }
    if (success) {
      return resolved;
    }
    throw rejected;
  } as unknown as T;
};
