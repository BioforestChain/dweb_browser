import { JsonlinesStream } from "./JsonlinesStream.cjs";

const $makeFetchExtends = <M extends unknown = unknown>(
  exts: $FetchExtends<M>
) => {
  return exts;
};
type $FetchExtends<E> = E & ThisType<Promise<Response> & E>; // Type of 'this' in methods is D & M

export const fetchExtends = $makeFetchExtends({
  async number() {
    const text = await this.text();
    return +text;
  },
  async text() {
    const response = await this;
    return response.text();
  },
  async boolean() {
    const text = await this.text();
    return text === "true"; // JSON.stringify(true)
  },
  async object<T>() {
    const response = await this;
    try {
      return (await response.json()) as T;
    } catch (err) {
      debugger;
      throw err;
    }
  },
  /** 将响应的内容解码成 jsonlines 格式 */
  async jsonlines<T = unknown>() {
    return (
      // 首先要能拿到数据流
      (await this.stream())
        // 先 解码成 utf8
        .pipeThrough(new TextDecoderStream())
        // 然后交给 jsonlinesStream 来处理
        .pipeThrough(new JsonlinesStream<T>())
    );
  },
  /** 获取 Response 的 body 为 ReadableStream */
  stream() {
    return this.then((res) => {
      const stream = res.body;
      if (stream == null) {
        throw new Error(`request ${res.url} could not by stream.`);
      }
      return stream;
    });
  },
});
