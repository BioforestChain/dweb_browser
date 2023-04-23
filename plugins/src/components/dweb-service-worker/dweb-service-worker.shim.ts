
import { dwebServiceWorkerPlugin } from "./dweb_service-worker.plugin.ts";
import { cacheGetter } from "../../helper/cacheGetter.ts";
import { DwebWorkerEventMap, UpdateControllerMap } from "./dweb-service-worker.type.ts";
import { BaseEvent, ListenerCallback, WindowListenerHandle } from "../base/BaseEvent.ts";

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



class DwebServiceWorker extends BaseEvent<keyof DwebWorkerEventMap> {

  constructor() {
    super(app_upgrade_watcher_kit)
  }

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

  constructor() {
    super(app_upgrade_watcher_kit)
  }

  @cacheGetter()
  get download() {
    return dwebServiceWorkerPlugin.update().download
  }

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


