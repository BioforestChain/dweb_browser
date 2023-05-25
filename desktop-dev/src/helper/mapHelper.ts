export const mapHelper = new (class {
  getOrPut<K extends object, V>(
    map: WeakMap<K, V>,
    key: K,
    putter: (key: K) => V
  ): V;
  getOrPut<K, V>(map: Map<K, V>, key: K, putter: (key: K) => V): V;
  getOrPut<K, V>(map: Map<K, V>, key: K, putter: (key: K) => V): V {
    if (map.has(key)) {
      return map.get(key)!;
    }
    const put = putter(key);
    map.set(key, put);
    return put;
  }
})();
