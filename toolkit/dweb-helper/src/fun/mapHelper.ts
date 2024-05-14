export const mapHelper = new (class {
  getOrPut<M extends WeakMap<any, any>, K extends $WeakMapKey<M>>(
    map: M,
    key: K,
    putter: (key: K) => $WeakMapVal<M>
  ): $WeakMapVal<M>;
  getOrPut<M extends Map<any, any>, K extends $MapKey<M>>(map: M, key: K, putter: $MapGetOrPutter<M, K>): $MapVal<M>;
  getOrPut<M extends Map<any, any>, K extends $MapKey<M>>(map: M, key: K, putter: $MapGetOrPutter<M, K>): $MapVal<M> {
    if (map.has(key)) {
      return map.get(key)!;
    }
    const put = putter(key);
    map.set(key, put);
    return put;
  }
  getAndRemove<M extends Map<any, any>, K extends $MapKey<M>>(map: M, key: K): $MapVal<M> | undefined {
    const val = map.get(key);
    if (map.delete(key)) {
      return val;
    }
  }
})();

type $MapKey<T> = T extends Map<infer K, any> ? K : never;
type $MapVal<T> = T extends Map<any, infer V> ? V : never;

type $WeakMapKey<T> = T extends WeakMap<infer K extends object, any> ? K : never;
type $WeakMapVal<T> = T extends WeakMap<any, infer V> ? V : never;
type $MapGetOrPutter<M, K> = (key: K) => $MapVal<M>;
