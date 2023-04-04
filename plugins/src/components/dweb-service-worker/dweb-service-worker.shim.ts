import { BasePlugin } from "../base/BasePlugin.ts";

class DwebServiceWorker extends BasePlugin {

  updateController = new UpdateController()

  tagName = "dweb-service-worker";

  constructor() {
    super("dweb-service-worker.sys.dweb")
  }

  /**拿到更新句柄 */
  update() {
    return this.updateController
  }

  /**关闭后端 */
  async close() {
    return await this.fetchApi("/close")
  }

  /**重启后端 */
  async restart() {
    return await this.fetchApi("/restart")
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

  }
}

class UpdateController extends BasePlugin {

  tagName = "dweb-update-controller";

  constructor() {
    super("dweb-update-controller.sys.dweb")
  }

  // 暂停
  async pause() {
    return await this.fetchApi("/pause")
  }
  // 重下
  async remuse(): Promise<boolean> {
    return await this.fetchApi("/remuse").boolean()
  }
  // 取消
  async cancel(): Promise<boolean> {
    return await this.fetchApi("/cancel").boolean()
  }

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

  }
}

interface DwebWorkerEventMap {
  updatefound: Event,
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
