export class AdaptersManager {
    constructor() {
        Object.defineProperty(this, "adapterOrderMap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
        Object.defineProperty(this, "orderdAdapters", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: []
        });
    }
    _reorder() {
        this.orderdAdapters = [...this.adapterOrderMap]
            .sort((a, b) => a[1] - b[1])
            .map((a) => a[0]);
    }
    get adapters() {
        return this.orderdAdapters;
    }
    append(adapter, order = 0) {
        this.adapterOrderMap.set(adapter, order);
        this._reorder();
        return () => this.remove(adapter);
    }
    remove(adapter) {
        if (this.adapterOrderMap.delete(adapter) != null) {
            this._reorder();
            return true;
        }
        return false;
    }
}
