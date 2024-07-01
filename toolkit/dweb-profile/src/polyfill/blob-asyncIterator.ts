if (Symbol.asyncIterator in Blob.prototype === false) {
  Object.defineProperty(Blob.prototype, Symbol.asyncIterator, {
    value: async function* (this: Blob) {
      const stram = this.stream();
      stram[Symbol.asyncIterator];
    },
  });
}

declare global {
  interface Blob {
    [Symbol.asyncIterator](): AsyncIterable<Uint8Array>;
  }
}
export {};
