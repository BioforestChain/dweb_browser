export const $makeExtends = <T>() => {
  return <M extends unknown = unknown>(exts: M & ThisType<T & M>) => {
    return exts;
  };
};
