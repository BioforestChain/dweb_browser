import { Readable, Writable } from "node:stream";

export const readableToWeb = (readable: Readable, options?: { strategy?: { highWaterMark?: number } }) => {
  // @ts-ignore
  return Readable.toWeb(readable, options) as ReadableStream<Uint8Array>;
};

export const writableToWeb = (writable: Writable) => {
  return Writable.toWeb(writable);
};
