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
}

// 测试代码 还需要判断 是否可以从这个初始化  __native_close_watcher_kit__ 这个对象
// iframe 无法实现 preload sceript
globalThis.__native_close_watcher_kit__ ? "" : Reflect.set(
  window,
  '__native_close_watcher_kit__',
  {
    allc: 0,
    _watchers: new Map(),
    _tasks: new Map(),
    registryToken: function(consumeToken: string){
      if (consumeToken === null || consumeToken === "") {
        throw new Error("CloseWatcher.registryToken invalid arguments");
      }
      const resolve = this._tasks.get(consumeToken)
      if(resolve === undefined) throw new Error('resolve === undefined');
      const id = this.allc++;
      resolve(id + "");
    },
    tryClose: function(id: string){
      const watcher = this._watchers.get(id);
      if(watcher === undefined) throw new Error('watcher === undefined');
      watcher.dispatchEvent(new Event("close"))
    }
  }
); 

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
}
export class CloseWatcher extends EventTarget {
  constructor() {
    super();
    void this.#init();
  }
  #id = new PromiseOut<string>();
  async #init() {
    const token = URL.createObjectURL(
      new Blob(["create-close-watcher"], { type: "text/html" })
    );

    const native_close_watcher_kit = globalThis.__native_close_watcher_kit__;
    const tasks = native_close_watcher_kit._tasks;
    const po = this.#id;
    // 注册回调
    tasks.set(token, po.resolve);
    // 注册指令
    native_close_watcher_kit.registryToken(token);
    /// 发起指令
    // 桌面端 是通过模拟器实现 back 所以不需要打开一个 window
    if('ontouchstart' in window){
      globalThis.open(token);
    }
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
interface CloseWatcherEventMap {
  close: CloseEvent;
}

// deno-lint-ignore no-explicit-any
if (typeof (globalThis as any)["CloseWatcher"] === "undefined") {
  Object.assign(globalThis, { CloseWatcher });
}
