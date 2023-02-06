import { simpleDecoder } from "./encoding.cjs";
import {
  streamReader,
  wrapAsyncGeneratorByAbortController,
} from "./readableStreamHelper.cjs";

const $makeFetchExtends = <M extends unknown = unknown>(
  exts: $FetchExtends<M>
) => {
  return exts;
};
type $FetchExtends<E> = E & ThisType<Promise<Response> & E>; // Type of 'this' in methods is D & M

async function* jsonlines<T>(
  stream_getter: Promise<ReadableStream<Uint8Array>>,
  abort_controller: AbortController
) {
  let json = "";
  for await (const chunk of streamReader(
    await stream_getter,
    abort_controller
  )) {
    json += simpleDecoder(chunk, "utf8");
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
}

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
      const object = (await response.json()) as T;
      return object;
    } catch (err) {
      debugger;
      throw err;
    }
  },
  /** 将响应的内容解码成 jsonlines 格式 */
  jsonlines<T = unknown>(abort_controller = new AbortController()) {
    return wrapAsyncGeneratorByAbortController(
      jsonlines<T>(this.stream(), abort_controller),
      abort_controller
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
