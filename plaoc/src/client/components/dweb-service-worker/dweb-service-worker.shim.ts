import { cacheGetter } from "../../helper/cacheGetter.ts";
import { ListenerCallback } from "../base/BaseEvent.ts";
import { ServiceWorkerFetchEvent } from "./FetchEvent.ts";
import { dwebServiceWorkerPlugin } from "./dweb-service-worker.plugin.ts";
import { DwebWorkerEventMap } from "./dweb-service-worker.type.ts";
class DwebServiceWorker extends EventTarget {
  plugin = dwebServiceWorkerPlugin;
  ws: WebSocket | undefined;
  constructor() {
    super();
    this.plugin.ipcPromise.then(ipc=>{
      ipc.onFetch((event) => {
        this.dispatchEvent(new ServiceWorkerFetchEvent(event,this.plugin))
      })
    })
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
    return super.addEventListener(eventName, listenerFunc as EventListenerOrEventListenerObject, options);
  }

  /**移除监听器 */
  override removeEventListener<K extends keyof DwebWorkerEventMap>(
    eventName: K,
    listenerFunc: ListenerCallback<DwebWorkerEventMap[K]>,
    options?: boolean | EventListenerOptions
  ) {
    return super.removeEventListener(eventName, listenerFunc as EventListenerOrEventListenerObject, options);
  }
}



export const dwebServiceWorker = new DwebServiceWorker();
