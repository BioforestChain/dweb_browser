export declare class AdaptersManager<T> {
    private readonly adapterOrderMap;
    private orderdAdapters;
    private _reorder;
    get adapters(): readonly T[];
    append(adapter: T, order?: number): () => boolean;
    remove(adapter: T): boolean;
}
