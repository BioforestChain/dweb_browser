// deno-lint-ignore ban-types
export const queue = <T extends Function>(fun: T) => {
  let queuer = Promise.resolve();
  // deno-lint-ignore no-explicit-any
  return function (...args: any[]) {
    return (queuer = queuer.finally(() => fun(...args)));
  };
};
