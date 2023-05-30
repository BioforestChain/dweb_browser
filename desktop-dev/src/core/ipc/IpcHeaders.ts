export class IpcHeaders extends Headers {
  init(key: string, value: string) {
    if (this.has(key)) {
      return;
    }
    this.set(key, value);
    return this;
  }
  toJSON() {
    const record: Record<string, string> = {};
    this.forEach((value, key) => {
      // 单词首字母大写
      record[key.replace(/\w+/g, (w) => w[0].toUpperCase() + w.slice(1))] =
        value;
    });
    return record;
  }
}
