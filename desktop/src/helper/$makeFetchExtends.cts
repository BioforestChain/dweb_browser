import { simpleDecoder } from "./encoding.cjs";

const $makeFetchExtends = <M extends unknown = unknown>(
  exts: $FetchExtends<M>
) => {
  return exts;
};
type $FetchExtends<E> = E & ThisType<Promise<Response> & E>; // Type of 'this' in methods is D & M

export const fetchExtends = $makeFetchExtends({
  async number() {
    const text = await this.string();
    return +text;
  },
  async string() {
    const response = await this;
    return response.text();
  },
  async boolean() {
    const text = await this.string();
    return text === "true"; // JSON.stringify(true)
  },
  async object<T>() {
    const response = await this;
    try {
      const object = (await response.json()) as T;
      return object;
    } catch (err) {
      debugger;
      throw err;
    }
  },
  async *jsonlines<T = unknown>() {
    const stream = await this.then((res) => {
      const stream = res.body;
      if (stream == null) {
        throw new Error(`request ${res.url} could not by stream.`);
      }
      return stream;
    });
    const reader = stream.getReader();

    let json = "";
    try {
      while (true) {
        const item = await reader.read();
        if (item.done) {
          break;
        }
        json += simpleDecoder(item.value, "utf8");
        while (json.includes("\n")) {
          const line_break_index = json.indexOf("\n");
          const line = json.slice(0, line_break_index);
          yield JSON.parse(line) as T;
          json = json.slice(line.length + 1);
        }
      }
      json = json.trim();
      if (json.length > 0) {
        yield JSON.parse(json) as T;
      }
    } catch (err) {
      debugger;
    }
  },
});
