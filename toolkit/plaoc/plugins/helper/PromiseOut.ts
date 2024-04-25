export class PromiseOut<T> {
  static resolve<T>(v: T) {
    const po = new PromiseOut<T>();
    po.resolve(v);
    return po;
  }

  static readonly PENDING = 0;
  static readonly RESOLVED = 1;
  static readonly REJECTED = 2;

  private _value?: T;
  private _readyState = PromiseOut.PENDING;
  get value() {
    return this._value;
  }
  get readyState() {
    return this._readyState;
  }
  public resolve!: (value: T | PromiseLike<T>) => void;
  public reject!: (reason?: any) => void;
  readonly promise = new Promise<T>((resolve, reject) => {
    this.resolve = resolve;
    this.reject = reject;
  }).then(
    (res) => {
      this._value = res;
      this._readyState = PromiseOut.RESOLVED;
      return res;
    },
    (err) => {
      this._readyState = PromiseOut.REJECTED;
      throw err;
    }
  );
}
