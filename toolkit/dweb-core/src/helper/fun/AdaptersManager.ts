export class AdaptersManager<T> {
  private readonly adapterOrderMap = new Map<T, number>();
  private orderdAdapters: T[] = [];
  private _reorder() {
    this.orderdAdapters = [...this.adapterOrderMap]
      .sort((a, b) => b[1] - a[1]) // order 越大越靠前
      .map((a) => a[0]);
  }
  get adapters() {
    return this.orderdAdapters as ReadonlyArray<T>;
  }
  /**
   *
   * @param adapter
   * @param order 越大优先级越高
   * @returns
   */
  append(adapter: T, order = 0) {
    this.adapterOrderMap.set(adapter, order);
    this._reorder();
    return () => this.remove(adapter);
  }

  remove(adapter: T) {
    if (this.adapterOrderMap.delete(adapter) != null) {
      this._reorder();
      return true;
    }
    return false;
  }
}
