import { PromiseOut } from "./PromiseOut.cjs";

export class LockManager {
  #queues = new Map<string, Promise<unknown>>();
  request<R>(name: string, callback: $LockGrantedCallback<R>) {
    let lock = this.#queues.get(name) ?? Promise.resolve();
    const r = new PromiseOut<R>();
    lock = lock.finally(async () => {
      try {
        r.resolve(await callback());
      } catch (err) {
        r.reject(err);
      }
      return r.promise;
    });
    this.#queues.set(name, lock);
    return r.promise as Promise<Awaited<R>>;
  }
}
export const locks = new LockManager();

// export interface $LockManager {
//   query(): Promise<LockManagerSnapshot>;
//   request<R>(
//     name: string,
//     callback: $LockGrantedCallback<R>
//   ): Promise<Awaited<R>>;
//   request<R>(
//     name: string,
//     options: LockOptions,
//     callback: $LockGrantedCallback<R>
//   ): Promise<Awaited<R>>;
// }
export interface $LockGrantedCallback<R = any> {
  (): R;
}
