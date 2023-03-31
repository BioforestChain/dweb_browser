import { BasePlugin } from "../base/BasePlugin.ts";

class DwebServiceWorker extends EventTarget {

  constructor() {
    super()
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

class UpdateController extends BasePlugin {

  tagName = "dweb-update-controller"

  async pause() {
    return true
  }
  async remuse(): Promise<boolean> {
    return true
  }
  async cancel(): Promise<boolean> {
    return true
  }

}

interface DwebWorkerEventMap {
  updatefound: Event,
  start: Event, // 监听启动
  progress: Event, // 进度每秒触发一次
  end: Event, // 结束
  cancel: Event, // 取消
}

// 控制进度的下载暂停取消
interface DwebWorkerControllerHandle {
  pause: Promise<boolean>,
  remuse: Promise<boolean>,
  cancel: Promise<boolean>
}


export const dwebServiceWorker = new DwebServiceWorker()
