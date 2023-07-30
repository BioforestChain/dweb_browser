import { toJsonlinesStream } from "../stream/jsonlinesStreamHelper.ts";

const $makeFetchExtends = <M extends unknown = unknown>(exts: $FetchExtends<M>) => {
  return exts;
};
type $FetchExtends<E> = E & ThisType<Promise<Response> & E>; // Type of 'this' in methods is D & M

export const fetchStreamExtends = $makeFetchExtends({
  /** 将响应的内容解码成 jsonlines 格式 */
  async jsonlines<T = unknown>() {
    return (
      // 首先要能拿到数据流
      toJsonlinesStream<T>(await this.stream())
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
