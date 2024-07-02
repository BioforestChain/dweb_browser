const sleep = (ms: number) => new Promise<void>((cb) => setTimeout(cb, ms));
// const waitIdea = () => new Promise<IdleDeadline>((cb) => requestIdleCallback(cb));
export class HalfFadeCache<K, V> {
  #cache = new Map<K, { time: number; hot: number; value: V }>();
  #loop = true;
  constructor(readonly halfFadeInternal: number = 5000) {
    (async () => {
      let preTime = Date.now();
      while (this.#loop) {
        // await waitIdea();
        const now = Date.now();
        const diff = now - preTime;
        for (const [key, wrapper] of this.#cache) {
          wrapper.time += diff;
          wrapper.hot >>= wrapper.time / this.halfFadeInternal;
          wrapper.time %= this.halfFadeInternal;
          if (wrapper.hot === 0) {
            this.#cache.delete(key);
          }
        }
        preTime = now;
        await sleep(500);
      }
    })();
  }
  set(key: K, value: V) {
    const wrapper = this.#cache.get(key) || { time: 0, hot: 0, value };
    wrapper.hot += 2 ** 10;
    this.#cache.set(key, wrapper);
    return this;
  }
  get(key: K) {
    return this.#cache.get(key)?.value;
  }
  getOrPut(key: K, defaultValue: (key: K) => V) {
    if (this.#cache.has(key)) {
      return this.#cache.get(key)!.value;
    }
    const value = defaultValue(key);
    this.set(key, value);
    return value;
  }
  delete(key: K) {
    return this.#cache.delete(key);
  }
  destroy() {
    this.#loop = false;
    this.#cache.clear();
  }
}
