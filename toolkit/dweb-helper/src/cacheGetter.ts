export const cacheGetter = () => {
  return (target: object, prop: string, desp: PropertyDescriptor) => {
    const source_fun = desp.get;
    if (source_fun === undefined) {
      throw new Error(`${target}.${prop} should has getter`);
    }
    desp.get = function () {
      const result = source_fun.call(this);
      if (desp.set) {
        desp.get = () => result;
      } else {
        delete desp.set;
        delete desp.get;
        desp.value = result;
        desp.writable = false;
      }
      Object.defineProperty(this, prop, desp);
      return result;
    };
    return desp;
  };
};

export class CacheGetter<T> {
  constructor(private getter: () => T) {}
  private _first = true;
  private _value?: T;
  get value() {
    if (this._first) {
      this._first = false;
      this._value = this.getter();
    }
    return this._value!;
  }
  reset() {
    this._first = true;
    this._value = undefined;
  }
}
