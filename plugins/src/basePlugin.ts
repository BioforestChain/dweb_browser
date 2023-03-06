/// <reference lib="dom" />

export class BasePlugin extends HTMLElement {

  protected listeners: { [eventName: string]: ListenerCallback[] } = {};
  protected windowListeners: { [eventName: string]: WindowListenerHandle } = {};


  // mmid:为对应组件的名称，proxy:为劫持对象的属性
  constructor(readonly mmid: string, readonly proxy: string) {
    super();
  }

  protected nativeFetch(url: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    if (url instanceof Request) {
      return this.nativeFetch(url, init)
    }
    return this.nativeFetch(new URL(url, this.mmid), init)
  }

  /**
    *  dwebview 注册一个监听事件
    * @param eventName 
    * @param listenerFunc 
    * @returns 
    */
  addListener(
    eventName: string,
    listenerFunc: ListenerCallback,
  ): Promise<PluginListenerHandle> & PluginListenerHandle {
    // 监听一个事件
    const listeners = this.listeners[eventName];
    if (!listeners) {
      this.listeners[eventName] = [];
    }

    this.listeners[eventName].push(listenerFunc);

    // 看看有没有添加过监听
    const windowListener = this.windowListeners[eventName];
    if (windowListener && !windowListener.registered) {
      this.addWindowListener(windowListener);
    }
    const remove = () => this.removeListener(eventName, listenerFunc);

    // deno-lint-ignore no-explicit-any
    const p: any = Promise.resolve({ remove });
    // 注册一个移除监听的方法
    Object.defineProperty(p, 'remove', {
      value: async () => {
        console.warn(`Using addListener() without 'await' is deprecated.`);
        await remove();
      },
    });

    return p;
  }

  /**添加一个监听器 */
  private addWindowListener(handle: WindowListenerHandle): void {
    // deno-lint-ignore no-window-prefix
    window.addEventListener(handle.windowEventName, handle.handler);
    handle.registered = true;
  }

  /**移除监听器 */
  private removeListener(
    eventName: string,
    listenerFunc: ListenerCallback,
  ) {
    const listeners = this.listeners[eventName];
    if (!listeners) {
      return;
    }

    const index = listeners.indexOf(listenerFunc);
    this.listeners[eventName].splice(index, 1);

    // 如果监听器为空，移除监听器
    if (!this.listeners[eventName].length) {
      this.removeWindowListener(this.windowListeners[eventName]);
    }
  }

  /**移除全局监听 */
  private removeWindowListener(handle: WindowListenerHandle): void {
    if (!handle) {
      return;
    }
    // deno-lint-ignore no-window-prefix
    window.removeEventListener(handle.windowEventName, handle.handler);
    handle.registered = false;
  }
  /**
   * 移动端通知调用由这里来触发前端注册的事件。
   * 比如某个组件注册了监听回调事件(移动端规定那些组件可以调用哪些事件)
   * 移动端触发了就通过这里踢一脚注册的组件
   * evalJavascript("")
   */
  // deno-lint-ignore no-explicit-any
  protected notifyListeners(eventName: string, data: any): void {
    console.log("🍙plugin#notifyListeners:", eventName, data)
    const listeners = this.listeners[eventName];
    if (listeners) {
      listeners.forEach(listener => listener(data));
    }
  }
  /**是否存在 */
  protected hasListeners(eventName: string): boolean {
    return !!this.listeners[eventName].length;
  }

}

// deno-lint-ignore no-explicit-any
export type ListenerCallback = (err: any, ...args: any[]) => void;

export interface WindowListenerHandle {
  registered: boolean;
  windowEventName: string;
  pluginEventName: string;
  // deno-lint-ignore no-explicit-any
  handler: (event: any) => void;
}

export interface PluginListenerHandle {
  remove: () => Promise<void>;
}
