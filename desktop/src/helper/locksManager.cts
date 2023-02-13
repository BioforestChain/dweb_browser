export const locks = navigator.locks as $LockManager;

export interface $LockManager {
  query(): Promise<LockManagerSnapshot>;
  request<R>(
    name: string,
    callback: $LockGrantedCallback<R>
  ): Promise<Awaited<R>>;
  request<R>(
    name: string,
    options: LockOptions,
    callback: $LockGrantedCallback<R>
  ): Promise<Awaited<R>>;
}
export interface $LockGrantedCallback<R = any> {
  (lock: Lock | null): R;
}
