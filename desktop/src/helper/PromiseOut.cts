export class PromiseOut<T> {
  static resolve<T>(v: T) {
    const po = new PromiseOut<T>();
    po.resolve(v);
    return po;
  }

  private _value?: T;
  get value() {
    return this._value;
  }
  public resolve!: (value: T | PromiseLike<T>) => void;
  public reject!: (reason?: any) => void;
  readonly promise = new Promise<T>((resolve, reject) => {
    this.resolve = resolve;
    this.reject = reject;
  }).then((res) => {
    this._value = res;
    return res;
  });
}
