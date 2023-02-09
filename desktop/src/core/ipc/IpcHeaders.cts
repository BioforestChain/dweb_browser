export class IpcHeaders extends Headers {
  init(key: string, value: string) {
    if (this.has(key)) {
      return;
    }
    this.set(key, value);
  }
  toJSON() {
    const record: Record<string, string> = {};
    this.forEach((value, key) => {
      record[key] = value;
    });
    return record;
  }
}
