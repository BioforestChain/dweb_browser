import { $Callback, createSignal } from "./createSignal.ts";
export class ChangeableMap<K, V> extends Map<K, V> {
  private _changeSignal = createSignal<$Callback<[this]>>();
  onChange = this._changeSignal.listen;
  emitChange = () => this._changeSignal.emit(this)
  override set(key: K, value: V) {
    if ((this.has(key) && this.get(key) === value) === false) {
      super.set(key, value);
      this._changeSignal.emit(this);
    }
    return this;
  }
  override delete(key: K): boolean {
    if (super.delete(key)) {
      this._changeSignal.emit(this);
      return true;
    }
    return false;
  }
  override clear(): void {
    if (this.size === 0) {
      return;
    }
    super.clear();
    this._changeSignal.emit(this);
  }
  /** 重置 清空所有的事件监听，清空所有的数据 */
  reset() {
    this._changeSignal.clear();
    this.clear();
  }
}
