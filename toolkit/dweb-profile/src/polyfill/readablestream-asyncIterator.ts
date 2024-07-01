if (Symbol.asyncIterator in ReadableStream.prototype === false) {
  function values<R>(this: ReadableStream<R>, { preventCancel = false } = {}) {
    const reader = this.getReader();
    return {
      async next() {
        try {
          const result = await reader.read();
          if (result.done) {
            reader.releaseLock();
          }
          return result;
        } catch (e) {
          reader.releaseLock();
          throw e;
        }
      },
      async return(value?: unknown) {
        if (!preventCancel) {
          const cancelPromise = reader.cancel(value);
          reader.releaseLock();
          await cancelPromise;
        } else {
          reader.releaseLock();
        }
        return { done: true, value };
      },
      [Symbol.asyncIterator]() {
        return this;
      },
    };
  }
  Object.defineProperty(ReadableStream.prototype, 'values', {
    enumerable: true,
    configurable: true,
    writable: true,
    value: values,
  });
  Object.defineProperty(ReadableStream.prototype, Symbol.asyncIterator, {
    enumerable: true,
    configurable: true,
    writable: true,
    value: values,
  });
}
declare global {
  interface ReadableStream<R> {
    values(): AsyncIterable<R>;
    [Symbol.asyncIterator](): AsyncIterable<R>;
  }
}
export {};
