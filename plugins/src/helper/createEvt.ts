// deno-lint-ignore no-explicit-any
export const createEvt = <CB extends $Callback<any[]> = $Callback>() => {
  const _cbs = new Set<CB>();
  const listen = (cb: CB): $OffListener => {
    _cbs.add(cb);
    return () => _cbs.delete(cb);
  };

  const emit = (...args: Parameters<CB>) => {
    for (const cb of _cbs) {
      cb.apply(null, args);
    }
  };
  const clear = () => {
    _cbs.clear();
  };
  return Object.assign([listen, emit] as const, { listen, emit, clear });
};

export type $Callback<ARGS extends unknown[] = [], RETURN = unknown> = (
  ...args: ARGS
) => RETURN;

export type $OffListener = () => boolean;
