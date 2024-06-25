declare global {
  interface PromiseConstructor {
    withResolvers<R>(): PromiseOut<R>;
  }
  type PromiseOut<R> = {
    resolve: (value: R | PromiseLike<R>) => void;
    reject: (reason?: any) => void;
    promise: Promise<R>;
  };
}

export const withResolvers =
  typeof Promise.withResolvers === "function"
    ? (Promise.withResolvers.bind(Promise) as typeof Promise.withResolvers)
    : (Promise.withResolvers = function withResolvers<R>() {
        var a,
          b,
          c = new Promise<R>(function (resolve, reject) {
            a = resolve;
            b = reject;
          });
        return { resolve: a, reject: b, promise: c };
      });
