import { u8aConcat } from "./binaryHelper.cjs";
import { createSignal, Signal } from "./createSignal.cjs";

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
  options: {
    signal?: AbortSignal;
  } = {}
) => {
  return _doRead(stream.getReader());
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
  controller!: ReadableStreamDefaultController<T>;
  stream = new ReadableStream<T>({
    start: (controller) => {
      this.controller = controller;
    },
    pull: () => {
      this._on_pull_signal?.emit();
    },
  });
  private _on_pull_signal?: Signal<$OnPull>;
  get onPull() {
    return (this._on_pull_signal ??= createSignal<$OnPull>()).listen;
  }
}
export type $OnPull = () => unknown;

export const streamFromCallback = <T extends (...args: any[]) => unknown>(
  cb: T,
  onCancel?: Promise<unknown>
) => {
  const stream = new ReadableStream<Parameters<T>>({
    start(controller) {
      onCancel?.then(() => controller.close());
      cb((...args: any[]) => {
        controller.enqueue(args as Parameters<T>);
      });
    },
  });

  return stream;
};
