
export const createSignal = <
  $Callback extends (...args: any[]) => unknown
>() => {
  const cbs = new Set<$Callback>();
  const bind = (cb: $Callback) => {
    cbs.add(cb);
    return () => cbs.delete(cb);
  };
  const emit = (...args: Parameters<$Callback>) => {
    for (const cb of cbs) {
      cb.apply(null, args);
    }
  };
  return { bind, emit } as const;
};
