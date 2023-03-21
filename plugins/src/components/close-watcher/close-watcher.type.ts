declare namespace globalThis {
  const __native_close_watcher_kit__: {
    create(): string;
    close(id: string): void;
    _watchers?: Map<string, EventTarget | string | ((token: string) => void)>;
  };
}

export const native_close_watcher_kit = globalThis.__native_close_watcher_kit__;
