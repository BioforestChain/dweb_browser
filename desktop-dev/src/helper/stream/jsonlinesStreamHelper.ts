import { JsonlinesStream } from "./JsonlinesStream.ts";
import { $AbortAble, $StreamReadAllOptions, streamRead, streamReadAll } from "./readableStreamHelper.ts";
export const toJsonlinesStream = <Type>(stream: ReadableStream<Uint8Array>) => {
  return (
    stream
      // 先 解码成 utf8
      .pipeThrough(new TextDecoderStream())
      // 然后交给 jsonlinesStream 来处理
      .pipeThrough(new JsonlinesStream<Type>())
  );
};

export const jsonlinesStreamRead = <Type>(stream: ReadableStream<Uint8Array>, options?: $AbortAble) => {
  return streamRead(toJsonlinesStream<Type>(stream), options);
};
export const jsonlinesStreamReadAll = <Type, Map, Result>(
  stream: ReadableStream<Uint8Array>,
  options?: $StreamReadAllOptions<Type, Map, Result>
) => {
  return streamReadAll(toJsonlinesStream<Type>(stream), options);
};
