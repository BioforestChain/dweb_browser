import { createMockModuleServerIpc } from "../../../common/websocketIpc.ts";
import { bindThis } from "../../helper/bindThis.ts";
import { ListenerCallback } from "../base/BaseEvent.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { $MMID } from "../index.ts";
import { UpdateControllerEvent, UpdateControllerMap } from "./dweb-update-controller.type.ts";

class UpdateControllerPlugin extends BasePlugin {
  readonly tagName = "dweb-update-controller";
  constructor() {
    super("jmm.browser.dweb");
  }
  // 获取监听的消息
  listen = new UpdateController();

  /**
   *  调出下载界面
   * @param metadataUrl 传递需要下载的metadata.json地址
   * @returns 
   */
  @bindThis
  async download(metadataUrl: string): Promise<boolean> {
    return this.fetchApi(`/install`, {
      search: {
        url: metadataUrl,
      },
    }).boolean();
  }

  // 暂停
  @bindThis
  async pause(): Promise<boolean> {
    return await this.fetchApi("/pause", {
      pathPrefix: this.listen.mmid,
    }).boolean();
  }
  // 恢复
  @bindThis
  async resume(): Promise<boolean> {
    return await this.fetchApi("/resume", {
      pathPrefix: this.listen.mmid,
    }).boolean();
  }
  // 取消
  @bindThis
  async cancel(): Promise<boolean> {
    return await this.fetchApi("/cancel", {
      pathPrefix: this.listen.mmid,
    }).boolean();
  }

  // /**
  //  * 比对版本
  //  * @param v1
  //  * @param v2
  //  * @returns
  //  */
  // compareVersion(v1: string, v2: string): number {
  //   const v1Parts = v1.split(".");
  //   const v2Parts = v2.split(".");

  //   for (let i = 0; i < Math.max(v1Parts.length, v2Parts.length); i++) {
  //     const v1Part = parseInt(v1Parts[i]) || 0;
  //     const v2Part = parseInt(v2Parts[i]) || 0;

  //     if (v1Part < v2Part) return -1;
  //     if (v1Part > v2Part) return 1;
  //   }

  //   return 0;
  // }
}

class UpdateController extends EventTarget {
  mmid: $MMID = "download.browser.dweb";
  readonly ipcPromise = this.createIpc();

  constructor() {
    super();
    this.ipcPromise.then((ipc) => {
      ipc.onEvent((event) => {
        if (event.name === UpdateControllerEvent.progress) {
          return this.dispatchEvent(new CustomEvent(event.name, { detail: { progress: event.text } }));
        }
        // start/end/cancel
        this.dispatchEvent(new Event(event.name));
      });
    });
  }

  private async createIpc() {
    const pub_url = await BasePlugin.public_url;
    const url = new URL(pub_url.replace(/^http:/, "ws:"));
    url.pathname = `${this.mmid}/listen`;
    const ipc = await createMockModuleServerIpc(url, {
      mmid: this.mmid,
      ipc_support_protocols: {
        cbor: false,
        protobuf: false,
        raw: false,
      },
      dweb_deeplinks: [],
      categories: [],
      name: this.mmid,
    });
    return ipc;
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
