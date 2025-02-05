export class IpcHeaders extends Headers {
  init(key: string, value: string) {
    if (this.has(key) === false) {
      this.set(key, value);
    }
    return this;
  }
  toJSON() {
    const record: Record<string, string> = Object.fromEntries(this.entries());
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
