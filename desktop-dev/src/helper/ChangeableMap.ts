import { $Callback, createSignal } from "./createSignal.ts";

type stateKey = "add"| "delete"
export type changeState<K> = {
  [key in stateKey]: K[];
}
export class ChangeableMap<K, V> extends Map<K, V> {
  private _changeSignal = createSignal<$Callback<[changeState<K>]>>();
  onChange = this._changeSignal.listen;
  emitChange = (state:changeState<K> = {add:[],delete:[]}) => this._changeSignal.emit(state)
  override set(key: K, value: V,signal = true) {
    if ((this.has(key) && this.get(key) === value) === false) {
      super.set(key, value);
      signal && this._changeSignal.emit({add:[key],delete:[]});
    }
    return this;
  }
  // 当emit为true才会触发信号
  override delete(key: K,signal = true): boolean {
    if (super.delete(key)) {
      signal && this._changeSignal.emit({add:[],delete:[key]});
      return true;
    }
    return false;
  }
  override clear(): void {
    if (this.size === 0) {
      return;
    }
    super.clear();
    this._changeSignal.emit({add:[],delete:[...this.keys()]});
  }
  /** 重置 清空所有的事件监听，清空所有的数据 */
  reset() {
    this._changeSignal.clear();
    this.clear();
  }
}
