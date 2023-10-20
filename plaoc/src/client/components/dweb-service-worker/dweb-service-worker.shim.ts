import { cacheGetter } from "../../helper/cacheGetter.ts";
import { ListenerCallback } from "../base/BaseEvent.ts";
import { ServiceWorkerFetchEvent } from "./FetchEvent.ts";
import { dwebServiceWorkerPlugin } from "./dweb-service-worker.plugin.ts";
import { DwebWorkerEventMap } from "./dweb-service-worker.type.ts";
class DwebServiceWorker extends EventTarget {
  plugin = dwebServiceWorkerPlugin;
  ws: WebSocket | undefined;

  messageQueue: ServiceWorkerFetchEvent[] = [];
  private isRegister = false;

  constructor() {
    super();
    this.plugin.ipcPromise.then((ipc) => {
      ipc.onFetch((event) => {
        console.log("收到消息", event.pathname, this.isRegister);
        const serviceEvent = new ServiceWorkerFetchEvent(event, this.plugin);
        if (!this.isRegister) {
          this.messageQueue.push(serviceEvent);
        }
        this.dispatchEvent(serviceEvent);
      });
    });
  }

  // 模拟messagePort 的start
  start() {
    console.log("触发缓存消息", this.messageQueue.length);
    while (this.messageQueue.length > 0) {
      const event = this.messageQueue.shift();
      console.log("派发消息", event, "当前消息：", this.messageQueue.length);
      event && this.dispatchEvent(event);
    }
  }

  @cacheGetter()
  get externalFetch() {
    return this.plugin.externalFetch;
  }

  @cacheGetter()
  get canOpenUrl() {
    return this.plugin.canOpenUrl;
  }

  @cacheGetter()
  get close() {
    return this.plugin.close;
  }

  @cacheGetter()
  get restart() {
    return this.plugin.restart;
  }

  /**
   *  dwebview 注册一个监听事件
   * @param eventName
   * @param listenerFunc
   * @returns
   */
  override addEventListener<K extends keyof DwebWorkerEventMap>(
    eventName: K,
    listenerFunc: ListenerCallback<DwebWorkerEventMap[K]>,
    options?: boolean | AddEventListenerOptions
  ) {
    void super.addEventListener(eventName, listenerFunc as EventListenerOrEventListenerObject, options);
    if (eventName === "fetch") {
      this.isRegister = true;
      this.start();
    }
  }

  /**移除监听器 */
  override removeEventListener<K extends keyof DwebWorkerEventMap>(
    eventName: K,
    listenerFunc: ListenerCallback<DwebWorkerEventMap[K]>,
    options?: boolean | EventListenerOptions
  ) {
    if (eventName === "fetch") {
      this.isRegister = false;
    }
    return super.removeEventListener(eventName, listenerFunc as EventListenerOrEventListenerObject, options);
  }
}

export const dwebServiceWorker = new DwebServiceWorker();
