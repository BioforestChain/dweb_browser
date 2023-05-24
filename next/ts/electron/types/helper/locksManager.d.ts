export declare class LockManager {
    #private;
    request<R>(name: string, callback: $LockGrantedCallback<R>): Promise<Awaited<R>>;
}
export declare const locks: LockManager;
export interface $LockGrantedCallback<R = any> {
    (): R;
}
