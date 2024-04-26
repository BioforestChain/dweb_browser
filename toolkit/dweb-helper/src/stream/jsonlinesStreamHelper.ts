import { JsonlinesStream } from "./JsonlinesStream.ts";
import { type $AbortAble, type $StreamReadAllOptions, streamRead, streamReadAll } from "./readableStreamHelper.ts";
export const binaryToJsonlinesStream = <Type>(stream: ReadableStream<Uint8Array>) => {
  return textToJsonlinesStream<Type>(
    stream
      // 先 解码成 utf8
      .pipeThrough(new TextDecoderStream())
  );
};
export const textToJsonlinesStream = <Type>(stream: ReadableStream<string>) => {
  return (
    stream
      // 交给 jsonlinesStream 来处理
      .pipeThrough(new JsonlinesStream<Type>())
  );
};

export const jsonlinesStreamReadBinary = <Type>(stream: ReadableStream<Uint8Array>, options?: $AbortAble) => {
  return streamRead(binaryToJsonlinesStream<Type>(stream), options);
};
export const jsonlinesStreamReadText = <Type>(stream: ReadableStream<string>, options?: $AbortAble) => {
  return streamRead(textToJsonlinesStream<Type>(stream), options);
};
export const jsonlinesStreamReadBinaryAll = <Type, Map, Result>(
  stream: ReadableStream<Uint8Array>,
  options?: $StreamReadAllOptions<Type, Map, Result>
) => {
  return streamReadAll(binaryToJsonlinesStream<Type>(stream), options);
};

export const jsonlinesStreamReadTextAll = <Type, Map, Result>(
  stream: ReadableStream<string>,
  options?: $StreamReadAllOptions<Type, Map, Result>
) => {
  return streamReadAll(textToJsonlinesStream<Type>(stream), options);
};
