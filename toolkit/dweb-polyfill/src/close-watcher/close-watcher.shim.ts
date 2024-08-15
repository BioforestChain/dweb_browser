/// <reference lib="dom"/>
import "./close-watcher.type.ts";
const __close_watcher__ = (() => {
  /// android 与 desktop 平台，可以注入 __native_close_watcher_kit__
  let native_close_watcher_kit = self.__native_close_watcher_kit__;
  // desktop 平台不能污染原对象，所以这里统一使用另外的对象来存储
  const native_close_watcher_exports = {
    watchers: new Map<string, CloseWatcher>(),
    tasks: new Map<string, (id: string) => void>(),
  };
  // 确保 __native_close_watcher_exports__ 在全局，使得 native 侧可以调到
  Object.assign(self, { __native_close_watcher_exports__: native_close_watcher_exports });

  /// iOS平台 使用消息通讯
  if (!native_close_watcher_kit && typeof webkit !== "undefined" && webkit.messageHandlers) {
    const closeWatcherChannel = webkit.messageHandlers.closeWatcher;
    native_close_watcher_kit = {
      init() {
        closeWatcherChannel.postMessage({ cmd: "init" });
      },
      registryToken(token: string) {
        closeWatcherChannel.postMessage({ cmd: "registryToken", token });
      },
      tryClose(id: string) {
        closeWatcherChannel.postMessage({ cmd: "tryClose", id });
      },
      tryDestroy(id: string) {
        closeWatcherChannel.postMessage({ cmd: "tryDestroy", destroy: id });
      },
    };
  }
  // 在脚本初始化阶段，重置状态
  native_close_watcher_kit.init();

  // @ts-ignore 假如导航的支持
  (typeof navigation === "object" ? navigation : null)?.addEventListener("navigate", (event) => {
    if (native_close_watcher_exports.watchers.size > 0) {
      event.intercept({
        async hanlder() {
          await [...native_close_watcher_exports.watchers.values()].pop()?.requestClose();
        },
      });
    }
  });

  /// 假如键盘的支持
  if (typeof document === "object")
    document.addEventListener("keypress", (event) => {
      if (event.key === "Escape") {
        [...native_close_watcher_exports.watchers.values()].pop()?.requestClose();
      }
    });

  class CloseWatcher extends EventTarget {
    #signal?: AbortSignal;
    constructor(options?: { signal?: AbortSignal }) {
      super();
      const signal = options?.signal;
      if (signal instanceof AbortSignal) {
        this.#signal = signal;
        signal.addEventListener("abort", () => {
          this.destroy();
        });
      }
      void this.#init();
    }
    #id_resolver!: (value: string) => void;
    #id = new Promise<string>((resolve) => (this.#id_resolver = resolve));
    async #init() {
      const token = URL.createObjectURL(new Blob(["create-close-watcher"], { type: "text/html" }));

      const tasks = native_close_watcher_exports.tasks;
      // 注册回调
      tasks.set(token, this.#id_resolver);
      // 注册指令，如果在移动端，会发起 self.open(token) ，从而获得
      native_close_watcher_kit.registryToken(token);

      // 等待响应
      const id = await this.#id;
      // 等到响应，删除注册的回掉
      tasks.delete(token);
      // 注册实例
      native_close_watcher_exports.watchers.set(id, this);
      console.log("close watcher created");

      this.addEventListener(
        "close",
        (event) => {
          console.log("close watcher closed");
          this.#onclose?.(event);
          native_close_watcher_exports.watchers.delete(id);
          this.#_closed = true;
        },
        { once: true }
      );
      this.addEventListener("close", (event) => {
        console.log("close watcher cancel");
        this.#oncancel?.(event);
        native_close_watcher_exports.watchers.delete(id);
        this.#_closed = true;
      });
    }
    #_closed = false;
    async close() {
      if (this.#_closed) {
        return;
      }
      const id = await this.#id;
      native_close_watcher_kit.tryClose(id);
    }
    async requestClose() {
      let canDoClose = !navigator.userActivation.isActive;
      if (!canDoClose && this.dispatchEvent(new Event("cancel", { cancelable: true }))) {
        canDoClose = true;
      }
      if (canDoClose) {
        await this.close();
      }
    }
    async destroy() {
      if (this.#_closed) {
        return;
      }
      const id = await this.#id;
      native_close_watcher_kit.tryDestroy(id);
    }
    #onclose?: (event: CloseEvent) => void;
    public get onclose() {
      return this.#onclose ?? null;
    }
    public set onclose(value) {
      if (typeof value === "function") {
        this.#onclose = value;
      } else {
        this.#onclose = undefined;
      }
    }
    #oncancel?: (event: Event) => void;
    public get oncancel() {
      return this.#oncancel ?? null;
    }
    public set oncancel(value) {
      if (typeof value === "function") {
        this.#oncancel = value;
      } else {
        this.#oncancel = undefined;
      }
    }
    override addEventListener<K extends keyof CloseWatcherEventMap>(
      type: K,
      // deno-lint-ignore no-explicit-any
      listener: (this: CloseWatcher, ev: CloseWatcherEventMap[K]) => any,
      options?: boolean | AddEventListenerOptions
    ): void;
    override addEventListener(
      type: string,
      listener: EventListenerOrEventListenerObject,
      options?: boolean | AddEventListenerOptions
    ): void;
    override addEventListener() {
      // deno-lint-ignore no-explicit-any
      return (super.addEventListener as any)(...arguments);
    }
    override removeEventListener<K extends keyof CloseWatcherEventMap>(
      type: K,
      // deno-lint-ignore no-explicit-any
      listener: (this: CloseWatcher, ev: CloseWatcherEventMap[K]) => any,
      options?: boolean | EventListenerOptions
    ): void;
    override removeEventListener(
      type: string,
      listener: EventListenerOrEventListenerObject,
      options?: boolean | EventListenerOptions
    ): void;
    override removeEventListener() {
      // deno-lint-ignore no-explicit-any
      return (super.addEventListener as any)(...arguments);
    }
  }

  // 强制移除 CloseWatcher，使用我们提供的版本
  Reflect.deleteProperty(self, "CloseWatcher");
  if (native_close_watcher_kit && typeof Reflect.get(self, "CloseWatcher") === "undefined") {
    Object.assign(self, { CloseWatcher: CloseWatcher });
  }
  return CloseWatcher;
})();
// 这里使用 export default，编译出来的时候不会有 exports 这些cjs对象
export default __close_watcher__;
