import { u8aConcat } from "./binaryHelper.cjs";
import { createSignal, Signal } from "./createSignal.cjs";

async function* _streamReader<T extends unknown>(
  stream: ReadableStream<T>,
  signal: AbortSignal
) {
  if (signal.aborted) {
    return;
  }
  const reader = stream.getReader();
  signal.addEventListener(
    "abort",
    (event) => {
      reader.cancel(event);
    },
    { once: true }
  );
  while (signal.aborted === false) {
    const item = await reader.read();
    if (item.done) {
      break;
    } else {
      yield item.value;
    }
  }
}

/** 将 AsyncGenerator 的控制函数与 AbortController 的接口绑定在一起 */
export const wrapAsyncGeneratorByAbortController = <AG extends AsyncGenerator>(
  async_generator: AG,
  abort_controller: AbortController
) => {
  const ag_return = async_generator.return.bind(async_generator);
  const ag_throw = async_generator.throw.bind(async_generator);
  type AG_Type = AG extends AsyncGenerator<infer T> ? T : never;
  const super_async_generator = Object.assign(async_generator, {
    return(value: Parameters<typeof ag_return>[0]) {
      abort_controller.abort(value);
      ag_return(value);
    },
    throw(e: unknown) {
      abort_controller.abort(e);
      ag_throw(e);
    },
    abort_controller,
    async each(
      onItem: (item: AG_Type) => unknown,
      onDone: () => unknown = () => {}
    ) {
      for await (const item of super_async_generator) {
        onItem(item as AG_Type);
      }
      onDone();
    },
  });
  return super_async_generator;
};

export const streamReader = <T extends unknown>(
  stream: ReadableStream<T>,
  abort_controller = new AbortController()
) => {
  return wrapAsyncGeneratorByAbortController(
    _streamReader(stream, abort_controller.signal),
    abort_controller
  );
};

export const streamReadAllBuffer = async (
  stream: ReadableStream<Uint8Array>
) => {
  const chunks: Uint8Array[] = [];
  for await (const chunk of streamReader<Uint8Array>(stream)) {
    chunks.push(chunk);
  }
  return u8aConcat(chunks);
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
  cb: T
) => {
  const stream = new ReadableStream<Parameters<T>>({
    start(controller) {
      cb((...args: any[]) => {
        controller.enqueue(args as Parameters<T>);
      });
    },
  });

  return stream;
};
