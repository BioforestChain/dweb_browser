export const importResolve = (path: string) => {
  if (import.meta.resolve !== undefined) {
    return import.meta.resolve(path);
  }
  return new URL(path, import.meta.url).href;
};
