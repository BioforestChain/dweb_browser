import { u8aConcat } from "./binaryHelper.ts";
import { $Callback, createSignal, Signal } from "./createSignal.ts";
interface $AbortAble {
  signal?: AbortSignal;
}

async function* _doRead<T extends unknown>(reader: ReadableStreamDefaultReader<T>, options?: $AbortAble) {
  const signal = options?.signal;
  if (signal !== undefined) {
    signal.addEventListener("abort", (reason) => reader.cancel(reason));
  }
  try {
    while (true) {
      const item = await reader.read();
      if (item.done) {
        break;
      }
      yield item.value;
    }
  } catch (err) {
    /// 如果是被throw，那么这个流会被打断，之后就无法在读取了
    reader.cancel(err);
  } finally {
    /// 如果只是return，那么这个流会释放 reader，如果前面执行了 cancel，那么之后再 getReader 也是 done 的情况
    reader.releaseLock();
  }
}

export const streamRead = <T extends unknown>(stream: ReadableStream<T>, options?: $AbortAble) => {
  return _doRead(stream.getReader(), options);
};

export const binaryStreamRead = (stream: ReadableStream<Uint8Array>, options?: $AbortAble) => {
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
      throw new Error(`fail to read bytes(${cache.length}/${size} byte) in stream`);
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
    complete?: (maps: T[]) => R;
  } = {}
) => {
  const maps: T[] = [];
  for await (const item of _doRead<I>(stream.getReader())) {
    if (options.map) {
      maps.push(options.map(item));
    }
  }

  type $Result = typeof options.complete extends undefined ? undefined : R;
  const result = options.complete?.(maps) as $Result;
  return {
    maps,
    result,
  };
};

export const streamReadAllBuffer = async (stream: ReadableStream<Uint8Array>) => {
  return (
    await streamReadAll(stream, {
      map(chunk) {
        return chunk;
      },
      complete(chunks) {
        return u8aConcat(chunks);
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
  stream: ReadableStream<T>;

  // deno-lint-ignore no-explicit-any
  private _on_cancel_signal?: Signal<$Callback<[/* reason: */ any]>>;
  get onCancel() {
    return (this._on_cancel_signal ??= createSignal()).listen;
  }
  private _on_pull_signal?: Signal<$OnPull>;
  get onPull() {
    return (this._on_pull_signal ??= createSignal<$OnPull>()).listen;
  }
}
export type $OnPull = () => unknown;
