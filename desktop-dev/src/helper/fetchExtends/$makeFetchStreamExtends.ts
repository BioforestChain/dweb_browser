import { JsonlinesStream } from "../JsonlinesStream.ts";

const $makeFetchExtends = <M extends unknown = unknown>(exts: $FetchExtends<M>) => {
  return exts;
};
type $FetchExtends<E> = E & ThisType<Promise<Response> & E>; // Type of 'this' in methods is D & M

export const fetchStreamExtends = $makeFetchExtends({
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
