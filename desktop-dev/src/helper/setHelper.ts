export const setHelper = new (class {
  intersect<T>(a: Iterable<T>, b: Iterable<T>) {
    const result = new Set(a);
    for (const item of b) {
      result.delete(item);
    }
    return result;
  }
})();
