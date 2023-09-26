import { bindThis } from "../../helper/bindThis.ts";
import { ListenerCallback } from "../base/BaseEvent.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { $DwebResult } from "../base/base.type.ts";
import { UpdateControllerMap } from "./dweb-update-controller.type.ts";

class UpdateControllerPlugin extends BasePlugin {
  readonly tagName = "dweb-update-controller";
  constructor() {
    super("jmm.browser.dweb");
  }
  // 获取监听的消息
  listen = new UpdateController();

  /**
   * 跳转下载新版本
   * 可以默认不传递medatadaUrl,系统会去识别第一次下载的地址去对比版本
   * 当需要下载新app或者下载地址更改的时候再去传递
   * @compatibility android/ios only
   * @returns
   */
  @bindThis
  async download(medatadaUrl?: string): Promise<boolean> {
    return this.fetchApi(`/install`, {
      search: {
        url: medatadaUrl,
      },
    }).boolean();
  }

  /**
   * 检查是否有新版本
   * @compatibility android/ios only
   * @returns
   */
  checkNewVersion() {
    return this.fetchApi(`/check`).object<$DwebResult>();
  }

  // 暂停
  @bindThis
  async pause(): Promise<boolean> {
    return await this.fetchApi("/pause").boolean();
  }
  // 恢复
  @bindThis
  async resume(): Promise<boolean> {
    return await this.fetchApi("/resume").boolean();
  }
  // 取消
  @bindThis
  async cancel(): Promise<boolean> {
    return await this.fetchApi("/cancel").boolean();
  }
}

class UpdateController extends EventTarget {
  constructor() {
    super();
  }

  /**
   *  dwebview 注册一个监听事件
   * @param eventName
   * @param listenerFunc
   * @returns
   */
  override addEventListener<K extends keyof UpdateControllerMap>(
    eventName: K,
    listenerFunc: ListenerCallback<UpdateControllerMap[K]>,
    options?: boolean | AddEventListenerOptions
  ) {
    return super.addEventListener(eventName, listenerFunc as EventListenerOrEventListenerObject, options);
  }

  /**移除监听器 */
  override removeEventListener<K extends keyof UpdateControllerMap>(
    eventName: K,
    listenerFunc: ListenerCallback<UpdateControllerMap[K]>,
    options?: boolean | EventListenerOptions
  ) {
    return super.removeEventListener(eventName, listenerFunc as EventListenerOrEventListenerObject, options);
  }
}

export const updateControllerPlugin = new UpdateControllerPlugin();
