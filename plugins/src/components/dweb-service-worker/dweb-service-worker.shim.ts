
import { dwebServiceWorkerPlugin } from "./dweb_service-worker.plugin.ts";
import { cacheGetter } from "../../helper/cacheGetter.ts";
import { DwebWorkerEventMap, ListenerCallback, UpdateControllerMap, WindowListenerHandle } from "./dweb-service-worker.type.ts";

declare namespace globalThis {
  const __app_upgrade_watcher_kit__: {
    /**
     * 该对象由 web 侧负责写入，由 native 侧去触发事件
     */
    // deno-lint-ignore no-explicit-any
    _listeners: { [eventName: string]: ListenerCallback<any>[] };
    _windowListeners: { [eventName: string]: WindowListenerHandle };
  }
}
// deno-lint-ignore no-explicit-any
(globalThis as any).__app_upgrade_watcher_kit__ = {}

const app_upgrade_watcher_kit = globalThis.__app_upgrade_watcher_kit__;

if (app_upgrade_watcher_kit) {
  app_upgrade_watcher_kit._listeners ??= {}
  app_upgrade_watcher_kit._windowListeners ??= {}
}

class BaseEvent<K extends keyof (UpdateControllerMap & DwebWorkerEventMap)> extends EventTarget {
  /**
 *  dwebview 注册一个监听事件
 * @param eventName 
 * @param listenerFunc 
 * @returns 
 */
  addEventListener(
    eventName: K,
    // deno-lint-ignore no-explicit-any
    listenerFunc: ListenerCallback<any>,
    options?: boolean | AddEventListenerOptions
  ): EventTarget {
    // 监听一个事件
    const listeners = app_upgrade_watcher_kit._listeners[eventName];
    if (!listeners) {
      app_upgrade_watcher_kit._listeners[eventName] = [];
    }

    app_upgrade_watcher_kit._listeners[eventName].push(listenerFunc);

    // 看看有没有添加过监听
    const windowListener = app_upgrade_watcher_kit._windowListeners[eventName];
    if (windowListener && !windowListener.registered) {
      this.addWindowListener(windowListener, options);
    }
    const remove = () => this.removeEventListener(eventName, listenerFunc);

    // deno-lint-ignore no-explicit-any
    const p: any = Promise.resolve({ remove });
    // 注册一个移除监听的方法
    Object.defineProperty(p, 'remove', {
      value: () => {
        console.warn(`Using addListener() without 'await' is deprecated.`);
        remove();
      },
    });

    return p;
  }

  /**添加一个监听器 */
  private addWindowListener(handle: WindowListenerHandle, options?: boolean | AddEventListenerOptions): void {
    super.addEventListener(handle.windowEventName, handle.handler, options)
    handle.registered = true;
  }

  /**移除监听器 */
  removeEventListener(
    eventName: K,
    // deno-lint-ignore no-explicit-any
    listenerFunc: ListenerCallback<any>,
    options?: boolean | EventListenerOptions
  ) {
    const listeners = app_upgrade_watcher_kit._listeners[eventName];
    if (!listeners) {
      return;
    }

    const index = listeners.indexOf(listenerFunc);
    app_upgrade_watcher_kit._listeners[eventName].splice(index, 1);

    // 如果监听器为空，移除监听器
    if (!app_upgrade_watcher_kit._listeners[eventName].length) {
      this.removeWindowListener(app_upgrade_watcher_kit._windowListeners[eventName], options);
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
  protected hasListeners(eventName: string): boolean {
    return !!app_upgrade_watcher_kit._listeners[eventName].length;
  }
}


class DwebServiceWorker extends BaseEvent<keyof DwebWorkerEventMap> {

  updateContoller = new UpdateController()

  @cacheGetter()
  get update() {
    return this.updateContoller
  }

  @cacheGetter()
  get close() {
    return dwebServiceWorkerPlugin.close
  }

  @cacheGetter()
  get restart() {
    return dwebServiceWorkerPlugin.restart
  }

  /**
*  dwebview 注册一个监听事件
* @param eventName 
* @param listenerFunc 
* @returns 
*/
  addEventListener<K extends keyof DwebWorkerEventMap>(
    eventName: K,
    listenerFunc: ListenerCallback<DwebWorkerEventMap[K]>,
    options?: boolean | AddEventListenerOptions
  ): EventTarget {
    return super.addEventListener(eventName, listenerFunc, options)
  }

  /**移除监听器 */
  removeEventListener<K extends keyof DwebWorkerEventMap>(
    eventName: K,
    listenerFunc: ListenerCallback<DwebWorkerEventMap[K]>,
    options?: boolean | EventListenerOptions
  ) { return super.removeEventListener(eventName, listenerFunc, options) }

}

class UpdateController extends BaseEvent<keyof UpdateControllerMap> {

  // 暂停
  @cacheGetter()
  get pause() {
    return dwebServiceWorkerPlugin.update().pause
  }
  // 重下
  @cacheGetter()
  get resume() {
    return dwebServiceWorkerPlugin.update().resume
  }
  // 取消
  @cacheGetter()
  get cancel() {
    return dwebServiceWorkerPlugin.update().cancel
  }
  // @cacheGetter()
  // get progress() {
  //   return dwebServiceWorkerPlugin.update().progress
  // }

  /**
*  dwebview 注册一个监听事件
* @param eventName 
* @param listenerFunc 
* @returns 
*/
  addEventListener<K extends keyof UpdateControllerMap>(
    eventName: K,
    listenerFunc: ListenerCallback<UpdateControllerMap[K]>,
    options?: boolean | AddEventListenerOptions
  ): EventTarget {
    return super.addEventListener(eventName, listenerFunc, options)
  }

  /**移除监听器 */
  removeEventListener<K extends keyof UpdateControllerMap>(
    eventName: K,
    listenerFunc: ListenerCallback<UpdateControllerMap[K]>,
    options?: boolean | EventListenerOptions
  ) { return super.removeEventListener(eventName, listenerFunc, options) }

}

export const dwebServiceWorker = new DwebServiceWorker()

// deno-lint-ignore no-explicit-any
if (typeof (globalThis as any)["DwebServiceWorker"] === "undefined") {
  Object.assign(globalThis, { DwebServiceWorker });
}

// deno-lint-ignore no-explicit-any
if (typeof (globalThis as any)["UpdateController"] === "undefined") {
  Object.assign(globalThis, { UpdateController });
}


