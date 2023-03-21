declare namespace globalThis {
  const __native_close_watcher_kit__: {
    create(): string;
    close(id: string): void;
    /**
     * 该对象由 web 侧负责写入
     */
    _watchers: Map<string, EventTarget>;
    /**
     * 该对象由 native 侧负责写入
     */
    _tasks: Map<
      string,
      { promise: Promise<string>; resolve: (id: string) => void }
    >;
  };
}

export const native_close_watcher_kit = globalThis.__native_close_watcher_kit__;
native_close_watcher_kit._watchers ??= new Map();
native_close_watcher_kit._tasks ??= new Map();
