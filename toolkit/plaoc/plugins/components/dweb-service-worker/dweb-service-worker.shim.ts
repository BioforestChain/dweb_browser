import { cacheGetter } from "../../helper/cacheGetter.ts";
import type { ListenerCallback } from "../base/base-event.ts";
import { ServiceWorkerFetchEvent } from "./FetchEvent.ts";
import { PlaocEvent } from "./IpcEvent.ts";
import { dwebServiceWorkerPlugin } from "./dweb-service-worker.plugin.ts";
import { eventHandle, type DwebWorkerEventMap } from "./dweb-service-worker.type.ts";

/**此处是消息分发中心，所有的app发给你的消息，都可以在这里监听到 */
class DwebServiceWorker extends EventTarget {
  plugin = dwebServiceWorkerPlugin;
  ws: WebSocket | undefined;

  messageQueue: ServiceWorkerFetchEvent[] = [];
  shortQueue: PlaocEvent[] = [];
  private isRegister = false;

  constructor() {
    super();
    // 事件分发中心
    this.plugin.ipcPromise.then((ipc) => {
      ipc.onFetch((fetchEvent) => {
        console.log("收到fetch消息", fetchEvent.pathname, this.isRegister);
        const serviceEvent = new ServiceWorkerFetchEvent(fetchEvent, this.plugin);
        if (!this.isRegister) {
          this.messageQueue.push(serviceEvent);
        }
        this.dispatchEvent(serviceEvent);
      });
      ipc.onEvent((event) => {
        console.log("收到Event消息", event.name, event.text, this.isRegister);
        const plaocEvent = new PlaocEvent(event.name, event.text);
        // shortcut
        if (!this.isRegister && event.name === eventHandle.shortcut) {
          this.shortQueue.push(plaocEvent);
        }

        this.dispatchEvent(plaocEvent);
      });
    });
  }

  // 模拟messagePort 的start
  private start() {
    console.log("触发缓存消息", this.messageQueue.length);
    while (this.messageQueue.length > 0) {
      const event = this.messageQueue.shift();
      console.log("派发消息", event, "当前消息：", this.messageQueue.length);
      event && this.dispatchEvent(event);
    }
  }

  private startShortcut() {
    console.log("触发startShortcut缓存消息", this.shortQueue.length);
    while (this.shortQueue.length > 0) {
      const event = this.shortQueue.shift();
      console.log("派发startShortcut消息", event, "当前消息：", this.shortQueue.length);
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
    this.isRegister = true;
    // 虽然有类型安全，但是这里还是做强验证
    if (eventName === "fetch") {
      this.start();
    } 
     if (eventName === "shortcut") {
      this.startShortcut();
    }
  }

  /**移除监听器 */
  override removeEventListener<K extends keyof DwebWorkerEventMap>(
    eventName: K,
    listenerFunc: ListenerCallback<DwebWorkerEventMap[K]>,
    options?: boolean | EventListenerOptions
  ) {
    this.isRegister = false;
    return super.removeEventListener(eventName, listenerFunc as EventListenerOrEventListenerObject, options);
  }
}

export const dwebServiceWorker = new DwebServiceWorker();
