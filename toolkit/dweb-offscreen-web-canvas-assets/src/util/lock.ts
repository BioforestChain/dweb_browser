export const withLock = async <R>(lockId: string, handler: () => Promise<R>) => {
  return navigator.locks.request(lockId, handler) as Promise<Awaited<R>>;
};
