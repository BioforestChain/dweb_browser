import { PromiseOut } from "npm:@dweb-browser/js-process@0.1.4";

export class PromiseToggle<T1, T2> {
  constructor(initState: { type: "open"; value: T1 } | { type: "close"; value: T2 }) {
    if (initState.type === "open") {
      this.toggleOpen(initState.value);
    } else {
      this.toggleClose(initState.value);
    }
  }
  private _open = new PromiseOut<T1>();
  private _close = new PromiseOut<T2>();
  waitOpen() {
    return this._open.promise;
  }
  waitClose() {
    return this._close.promise;
  }
  get isOpen() {
    return this._open.is_resolved;
  }
  get isClose() {
    return this._close.is_resolved;
  }
  get openValue() {
    return this._open.value;
  }
  get closeValue() {
    return this._close.value;
  }
  /**
   * 切换到开的状态
   * @param value
   * @returns
   */
  toggleOpen(value: T1) {
    if (this._open.is_resolved) {
      return;
    }
    this._open.resolve(value);
    if (this._close.is_resolved) {
      this._close = new PromiseOut();
    }
  }
  /**
   * 切换到开的状态
   * @param value
   * @returns
   */
  toggleClose(value: T2) {
    if (this._close.is_resolved) {
      return;
    }
    this._close.resolve(value);
    if (this._open.is_resolved) {
      this._open = new PromiseOut();
    }
  }
}
