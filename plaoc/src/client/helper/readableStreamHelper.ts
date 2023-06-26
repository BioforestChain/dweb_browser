import { u8aConcat } from "./binaryHelper.ts";
import { createSignal, Signal } from "./createSignal.ts";

async function* _doRead<T extends unknown>(
  reader: ReadableStreamDefaultReader<T>
) {
  try {
    while (true) {
      const item = await reader.read();
      if (item.done) {
        break;
      }
      yield item.value;
    }
  } finally {
    reader.releaseLock();
  }
}

export const streamRead = <T extends unknown>(
  stream: ReadableStream<T>,
  _options: {
    signal?: AbortSignal;
  } = {}
) => {
  return _doRead(stream.getReader());
};

export const binaryStreamRead = (
  stream: ReadableStream<Uint8Array>,
  options: {
    signal?: AbortSignal;
  } = {}
) => {
  const reader = streamRead(stream, options);
  let done = false;
  let cache = new Uint8Array(0);
  const appendToCache = async () => {
    const item = await reader.next();
    if (item.done) {
      done = true;
      return false;
    } else {
      cache = u8aConcat([cache, item.value]);
      return true;
    }
  };
  const available = async (): Promise<number> => {
    if (cache.length > 0) {
      return cache.length;
    }
    if (done) {
      return -1;
    }
    await appendToCache();
    return available();
  };
  const readBinary = async (size: number): Promise<Uint8Array> => {
    if (cache.length >= size) {
      const result = cache.subarray(0, size);
      cache = cache.subarray(size);
      return result;
    }
    if (await appendToCache()) {
      return readBinary(size);
    } else {
      throw new Error(
        `fail to read bytes(${cache.length}/${size} byte) in stream`
      );
    }
  };
  const u32 = new Uint32Array(1);
  const u32_u8 = new Uint8Array(u32.buffer);
  const readInt = async () => {
    const intBuf = await readBinary(4);
    u32_u8.set(intBuf);
    return u32[0];
  };
  return Object.assign(reader, {
    available,
    readBinary,
    readInt,
  });
};

export const streamReadAll = async <I extends unknown, T, R>(
  stream: ReadableStream<I>,
  options: {
    map?: (item: I) => T;
    complete?: (items: I[], maps: T[]) => R;
  } = {}
) => {
  const items: I[] = [];
  const maps: T[] = [];
  for await (const item of _doRead<I>(stream.getReader())) {
    items.push(item);
    if (options.map) {
      maps.push(options.map(item));
    }
  }

  type $Result = typeof options.complete extends undefined ? undefined : R;
  const result = options.complete?.(items, maps) as $Result;
  return {
    items,
    maps,
    result,
  };
};

export const streamReadAllBuffer = async (
  stream: ReadableStream<Uint8Array>
) => {
  return (
    await streamReadAll(stream, {
      complete(items) {
        return u8aConcat(items);
      },
    })
  ).result;
};
export class ReadableStreamOut<T> {
  constructor(readonly strategy?: QueuingStrategy<T>) {
    this.stream = new ReadableStream<T>(
      {
        cancel: (reason) => {
          this._on_cancel_signal?.emit(reason);
        },
        start: (controller) => {
          this.controller = controller;
        },
        pull: () => {
          this._on_pull_signal?.emit();
        },
      },
      this.strategy
    );
  }
  controller!: ReadableStreamDefaultController<T>;
  readonly stream: ReadableStream;
  private _on_cancel_signal?: Signal<$OnCancel>;
  get onCancel() {
    return (this._on_cancel_signal ??= createSignal<$OnCancel>()).listen;
  }
  private _on_pull_signal?: Signal<$OnPull>;
  get onPull() {
    return (this._on_pull_signal ??= createSignal<$OnPull>()).listen;
  }
}
export type $OnPull = () => unknown;
// deno-lint-ignore no-explicit-any
export type $OnCancel = (reason: any) => unknown;

// deno-lint-ignore no-explicit-any
export const streamFromCallback = <T extends (...args: any[]) => unknown>(
  cb: T,
  onCancel?: Promise<unknown>
) => {
  const stream = new ReadableStream<Parameters<T>>({
    start(controller) {
      onCancel?.then(() => controller.close());
      // deno-lint-ignore no-explicit-any
      cb((...args: any[]) => {
        controller.enqueue(args as Parameters<T>);
      });
    },
  });

  return stream;
};
