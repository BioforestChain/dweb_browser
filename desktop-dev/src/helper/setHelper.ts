export const setHelper = new (class {
  intersect<T>(a: Iterable<T>, b: Iterable<T>) {
    const result = new Set(a);
    for (const item of b) {
      result.delete(item);
    }
    return result;
  }
  equals<T>(a: Iterable<T>, b: Iterable<T>) {
    const diff = new Set(a);
    for (const item of b) {
      if (diff.delete(item) === false) {
        return false;
      }
    }
    return true;
  }
})();
