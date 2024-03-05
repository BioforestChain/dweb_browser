export class IpcHeaders extends Headers {
  init(key: string, value: string) {
    if (this.has(key) === false) {
      this.set(key, value);
    }
    return this;
  }
  toJSON() {
    const record: Record<string, string> = {};
    this.forEach((value, key) => {
      // 单词首字母大写
      record[key.replace(/\w+/g, (w) => w[0].toUpperCase() + w.slice(1))] = value;
    });
    return record;
  }
}
type $IpcHeaders = InstanceType<typeof IpcHeaders>;

export const cors = (headers: $IpcHeaders) => {
  headers.init("Access-Control-Allow-Origin", "*");
  headers.init("Access-Control-Allow-Headers", "*"); // 要支持 X-Dweb-Host
  headers.init("Access-Control-Allow-Methods", "*");
  // headers.init("Connection", "keep-alive");
  // headers.init("Transfer-Encoding", "chunked");
  return headers;
};
