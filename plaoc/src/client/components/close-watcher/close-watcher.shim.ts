import { PromiseOut } from "../../helper/PromiseOut.ts";
declare namespace globalThis {
  const __native_close_watcher_kit__: {
    /**
     * 将一个url注册成token，在拦截到要打开此url的时候，覆盖原本open的行为，改成 创建一个 CloseWatcher
     * 基于 open 接口，原因是 CloseWatcher 的创建本身要基于浏览器的 UserActivation 中“消耗用户激活”的机制
     */
    registryToken(token: string): void;
    /**
     * 尝试关闭 CloseWatcher，可能会触发 cancel 事件，取决于平台的兼容程度
     */
    tryClose(id: string): void;
    /**
     * 该对象由 web 侧负责写入，由 native 侧去触发事件
     */
    _watchers: Map<string, EventTarget>;
    /**
     * 该对象由 web 侧负责写入，由 native 侧去调用
     */
    _tasks: Map<string, (id: string) => void>;
  };
  function open(url: string): Window;

  const CloseWatcher: typeof CloseWatcherShim;
}

let native_close_watcher_kit = globalThis.__native_close_watcher_kit__;

if (native_close_watcher_kit) {
  native_close_watcher_kit._watchers ??= new Map();
  native_close_watcher_kit._tasks ??= new Map();
} else if ("webkit" in globalThis) {
  // 仅适用于iOS平台
  Object.assign(globalThis, {
    __native_close_watcher_kit__: {
      registryToken(token: string) {
        try {
          // deno-lint-ignore no-explicit-any
          (globalThis as any).webkit.messageHandlers.closeWatcher.postMessage({
            token,
          });
        } catch {
          // 非iOS平台才有可能会触发
        }
      },

      tryClose(id: string) {
        try {
          // deno-lint-ignore no-explicit-any
          (globalThis as any).webkit.messageHandlers.closeWatcher.postMessage({
            id,
          });
        } catch {
          // 非iOS平台才有可能会触发
        }
      },
      _watchers: new Map(),
      _tasks: new Map(),
    },
  });
  native_close_watcher_kit = globalThis.__native_close_watcher_kit__;
} else {
  /// 桌面 平台使用 esc 按钮作为返回键
  const consuming = new Set<String>();
  const watchers = new Array<Watcher>();
  class Watcher {
    static #acc_id = 0;
    private _destoryed = false;
    readonly id = `#cw-${Watcher.#acc_id++}`;
    tryClose() {
      if (this._destoryed) {
        return false;
      }
      native_close_watcher_kit._watchers.get(this.id)?.dispatchEvent(new CloseEvent("close"));
      this._destoryed = true;
      return true;
    }
  }
  const closeWatcherController = new (class CloseWatcher {
    /**
     * 申请一个 CloseWatcher
     */
    apply(isUserGesture: boolean) {
      if (isUserGesture || watchers.length === 0) {
        const watcher = new Watcher();
        watchers.push(watcher);
      }
      return watchers.at(-1)!;
    }
    resolveToken(consumeToken: string, watcher: Watcher) {
      native_close_watcher_kit._tasks.get(consumeToken)?.(watcher.id);
    }
    get canClose() {
      return watchers.length > 0;
    }
    close(watcher = watchers.at(-1)) {
      if (watcher?.tryClose()) {
        const index = watchers.indexOf(watcher);
        if (index !== -1) {
          watchers.splice(index, 1);
          return true;
        }
      }
      return false;
    }
  })();
  // @ts-ignore
  if (typeof navigation === "object") {
    // @ts-ignore
    navigation.addEventListener("navigate", (event) => {
      if (closeWatcherController.canClose) {
        event.intercept({
          async hanlder() {
            closeWatcherController.close();
          },
        });
      }
    });
  }
  document.addEventListener("keypress", (event) => {
    if (event.key === "Escape") {
      console.log("Esc键被按下");
      closeWatcherController.close();
    }
  });

  Object.assign(globalThis, {
    __native_close_watcher_kit__: {
      async registryToken(token: string) {
        consuming.add(token);
        /// 模拟移动端的 open(token)
        if ((await (await fetch(token)).text()) === "create-close-watcher") {
          const watcher = closeWatcherController.apply(navigator.userActivation.isActive);
          closeWatcherController.resolveToken(token, watcher);
        }
      },

      tryClose(id: string) {
        for (const w of watchers.slice()) {
          if (w.id === id) {
            closeWatcherController.close(w);
          }
        }
      },
      _watchers: new Map(),
      _tasks: new Map(),
    },
  });
  native_close_watcher_kit = globalThis.__native_close_watcher_kit__;
}
class CloseWatcherShim extends EventTarget {
  constructor() {
    super();
    void this.#init();
  }
  #id = new PromiseOut<string>();
  async #init() {
    const token = URL.createObjectURL(new Blob(["create-close-watcher"], { type: "text/html" }));

    const native_close_watcher_kit = globalThis.__native_close_watcher_kit__;
    const tasks = native_close_watcher_kit._tasks;
    const po = this.#id;
    // 注册回调
    tasks.set(token, po.resolve);
    // 注册指令，如果在移动端，会发起 window.open(token) ，从而获得
    native_close_watcher_kit.registryToken(token);

    // 等待响应
    const id = await po.promise;
    // 注册实例
    native_close_watcher_kit._watchers.set(id, this);
    console.log("close watcher created");

    this.addEventListener(
      "close",
      (event) => {
        console.log("close watcher closed");
        this.#onclose?.(event);
        native_close_watcher_kit._watchers.delete(id);
        this._closed = true;
      },
      { once: true }
    );
  }
  private _closed = false;
  async close() {
    if (this._closed) {
      return;
    }
    const id = await this.#id.promise;
    const native_close_watcher_kit = globalThis.__native_close_watcher_kit__;
    native_close_watcher_kit.tryClose(id);
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
  override addEventListener<K extends keyof CloseWatcherEventMap>(
    type: K,
    // deno-lint-ignore no-explicit-any
    listener: (this: CloseWatcherShim, ev: CloseWatcherEventMap[K]) => any,
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
    listener: (this: CloseWatcherShim, ev: CloseWatcherEventMap[K]) => any,
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
interface CloseWatcherEventMap {
  close: CloseEvent;
}

// deno-lint-ignore no-explicit-any
if (typeof globalThis.CloseWatcher === "undefined") {
  Object.assign(globalThis, { CloseWatcher: CloseWatcherShim });
}

export { CloseWatcherShim as CloseWatcher };
