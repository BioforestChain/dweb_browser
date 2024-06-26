export const setHelper = new (class {
  sort<T>(a: Iterable<T>, compareFn?: (a: T, b: T) => number) {
    return [...a].sort(compareFn);
  }
  union<T>(a: Iterable<T>, b: Iterable<T>) {
    const result = new Set(a);
    for (const item of b) {
      result.add(item);
    }
    return result;
  }
  intersect<T>(a: Iterable<T>, b: Iterable<T>) {
    const aSet = new Set(a);
    const bSet = new Set(b);
    const result = new Set([...aSet, ...bSet]);
    for (const item of result) {
      if (false === (aSet.has(item) && bSet.has(item))) {
        result.delete(item);
      }
    }
    return result;
  }
  subtract<T>(a: Iterable<T>, b: Iterable<T>) {
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
  add<T>(target: Set<T>, value: T) {
    if (target.has(value)) {
      return false;
    }
    target.add(value);
    return true;
  }
})();
