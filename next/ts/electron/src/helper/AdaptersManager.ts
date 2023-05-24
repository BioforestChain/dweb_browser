export class AdaptersManager<T> {
  private readonly adapterOrderMap = new Map<T, number>();
  private orderdAdapters: T[] = [];
  private _reorder() {
    this.orderdAdapters = [...this.adapterOrderMap]
      .sort((a, b) => a[1] - b[1])
      .map((a) => a[0]);
  }
  get adapters() {
    return this.orderdAdapters as ReadonlyArray<T>;
  }
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
