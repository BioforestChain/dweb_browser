
import { dwebServiceWorkerPlugin } from "./dweb_service-worker.plugin.ts";
import { cacheGetter } from "../../helper/cacheGetter.ts";

declare namespace globalThis {
  const __app_upgrade_watcher_kit__: {
    /**
     * 该对象由 web 侧负责写入，由 native 侧去触发事件
     */
    _events: Map<string, EventTarget>;
  }
}

const app_upgrade_watcher_kit = globalThis.__app_upgrade_watcher_kit__;

if (app_upgrade_watcher_kit) {
  app_upgrade_watcher_kit._events ??= new Map()
}

class DwebServiceWorker extends EventTarget {

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


  addEventListener<K extends keyof DwebWorkerEventMap>(
    type: K,
    // deno-lint-ignore no-explicit-any
    listener: (this: DwebServiceWorker, ev: DwebWorkerEventMap[K]) => any,
    options?: boolean | AddEventListenerOptions
  ): void;
  addEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject,
    options?: boolean | AddEventListenerOptions
  ): void;


  addEventListener() {
    // deno-lint-ignore no-explicit-any
    return (super.addEventListener as any)(...arguments);
  }

  removeEventListener<K extends keyof DwebWorkerEventMap>(
    type: K,
    // deno-lint-ignore no-explicit-any
    listener: (this: DwebServiceWorker, ev: DwebWorkerEventMap[K]) => any,
    options?: boolean | EventListenerOptions
  ): void;
  removeEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject,
    options?: boolean | EventListenerOptions
  ): void;
  removeEventListener() {
    // deno-lint-ignore no-explicit-any
    return (super.addEventListener as any)(...arguments);
  }
}

class UpdateController extends EventTarget {


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

  addEventListener<K extends keyof UpdateControllerMap>(
    type: K,
    // deno-lint-ignore no-explicit-any
    listener: (this: DwebServiceWorker, ev: UpdateControllerMap[K]) => any,
    options?: boolean | AddEventListenerOptions
  ): void;
  addEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject,
    options?: boolean | AddEventListenerOptions
  ): void;

  addEventListener() {
    // deno-lint-ignore no-explicit-any
    return (super.addEventListener as any)(...arguments);
  }
  removeEventListener<K extends keyof UpdateControllerMap>(
    type: K,
    // deno-lint-ignore no-explicit-any
    listener: (this: DwebServiceWorker, ev: UpdateControllerMap[K]) => any,
    options?: boolean | EventListenerOptions
  ): void;
  removeEventListener(
    type: string,
    listener: EventListenerOrEventListenerObject,
    options?: boolean | EventListenerOptions
  ): void;
  removeEventListener() {
    // deno-lint-ignore no-explicit-any
    return (super.addEventListener as any)(...arguments);
  }
}

interface DwebWorkerEventMap {
  updatefound: Event, // 更新或重启的时候触发
  fetch: Event,
  onFetch: Event
}

interface UpdateControllerMap {
  start: Event, // 监听启动
  progress: Event, // 进度每秒触发一次
  end: Event, // 结束
  cancel: Event, // 取消
}

export const dwebServiceWorker = new DwebServiceWorker()
