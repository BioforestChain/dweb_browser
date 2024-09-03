export class BaseEvent<K extends string> extends EventTarget {
  app_kit: Tkit;

  constructor(lnstenObj: Tkit) {
    super();
    this.app_kit = lnstenObj;
  }

  /**
   *  dwebview 注册一个监听事件
   * @param eventName
   * @param listenerFunc
   * @returns
   */
  override addEventListener(
    eventName: K,
    // deno-lint-ignore no-explicit-any
    listenerFunc: ListenerCallback<any>,
    options?: boolean | AddEventListenerOptions
  ): EventTarget {
    // 监听一个事件
    const listeners = this.app_kit._listeners[eventName];
    if (!listeners) {
      this.app_kit._listeners[eventName] = [];
    }

    this.app_kit._listeners[eventName].push(listenerFunc);

    // 看看有没有添加过监听
    const windowListener = this.app_kit._windowListeners[eventName];
    if (windowListener && !windowListener.registered) {
      this.addWindowListener(windowListener, options);
    }
    const remove = () => this.removeEventListener(eventName, listenerFunc);

    // deno-lint-ignore no-explicit-any
    const p: any = Promise.resolve({ remove });
    // 注册一个移除监听的方法
    Object.defineProperty(p, "remove", {
      value: () => {
        console.warn(`Using addListener() without 'await' is deprecated.`);
        remove();
      },
    });

    return p;
  }

  /**添加一个监听器 */
  private addWindowListener(handle: WindowListenerHandle, options?: boolean | AddEventListenerOptions): void {
    super.addEventListener(handle.windowEventName, handle.handler, options);
    handle.registered = true;
  }

  /**移除监听器 */
  override removeEventListener(
    eventName: K,
    // deno-lint-ignore no-explicit-any
    listenerFunc: ListenerCallback<any>,
    options?: boolean | EventListenerOptions
  ) {
    const listeners = this.app_kit._listeners[eventName];
    if (!listeners) {
      return;
    }

    const index = listeners.indexOf(listenerFunc);
    this.app_kit._listeners[eventName].splice(index, 1);

    // 如果监听器为空，移除监听器
    if (!this.app_kit._listeners[eventName].length) {
      this.removeWindowListener(this.app_kit._windowListeners[eventName], options);
    }
  }

  /**移除全局监听 */
  private removeWindowListener(handle: WindowListenerHandle, options?: boolean | EventListenerOptions): void {
    if (!handle) {
      return;
    }
    super.removeEventListener(handle.windowEventName, handle.handler, options);
    handle.registered = false;
  }
  /**是否存在 */
  protected hasListeners(eventName: K): boolean {
    return !!this.app_kit._listeners[eventName].length;
  }

  // deno-lint-ignore no-explicit-any
  protected notifyListeners(eventName: K, data: any): void {
    const listeners = this.app_kit._listeners[eventName];
    if (listeners) {
      listeners.forEach((listener) => listener(data));
    }
  }
}

export interface PluginListenerHandle {
  remove: () => Promise<void>;
}

export interface WindowListenerHandle {
  registered: boolean;
  windowEventName: string;
  pluginEventName: string;
  // deno-lint-ignore no-explicit-any
  handler: (event: any) => void;
}

export type ListenerCallback<T> = (event: T) => Promise<void> | void | null;

export type Tkit = {
  /**
   * 该对象由 web 侧负责写入，由 native 侧去触发事件
   */
  // deno-lint-ignore no-explicit-any
  _listeners: { [eventName: string]: ListenerCallback<any>[] };
  _windowListeners: { [eventName: string]: WindowListenerHandle };
};
