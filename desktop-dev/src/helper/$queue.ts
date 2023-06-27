export const queue = <T extends Function>(fun: T) => {
  let queuer = Promise.resolve();
  return function (...args: any[]) {
    return (queuer = queuer.finally(() => fun(...args)));
  };
};
