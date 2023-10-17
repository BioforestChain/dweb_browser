
export class Store<T> {
  constructor() {}
  data?: T = undefined;
  // 读取 JSON
  get() {
    return this.data;
  }

  // 写入 JSON
  async set(obj: T) {
    this.data = obj;
  }
}
// export const urlStore = new Store<{ [key in X_PLAOC_QUERY]?: string }>();
