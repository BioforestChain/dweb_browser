import { bindThis } from "../../helper/bindThis.ts";
import { streamRead } from "../../helper/readableStreamHelper.ts";
import { BasePlugin } from "../base/BasePlugin.ts";

export class DwebServiceWorkerPlugin extends BasePlugin {
  tagName = "dweb-service-worker";

  updateController = new UpdateControllerPlugin()

  constructor() {
    super("service-worker.nativeui.sys.dweb")
  }
  /**拿到更新句柄 */
  @bindThis
  update() {
    return this.updateController
  }

  /**关闭后端 */
  @bindThis
  async close() {
    return await this.fetchApi("/close")
  }

  /**重启后端 */
  @bindThis
  async restart() {
    return await this.fetchApi("/restart")
  }

}


class UpdateControllerPlugin extends BasePlugin {
  tagName = "dweb-update-controller";

  progressNum = 0;

  constructor() {
    super("jmm.sys.dweb")
    this.init()
  }

  async init() {
    console.log(`UpdateControllerPlugin =>${await this.getMMid()} , ${this.mmid}`)
  }

  // 暂停
  @bindThis
  async pause() {
    return await this.fetchApi("/pause")
  }
  // 重下
  @bindThis
  async resume(): Promise<boolean> {
    return await this.fetchApi("/resume").boolean()
  }
  // 取消
  @bindThis
  async cancel(): Promise<boolean> {
    return await this.fetchApi("/cancel").boolean()
  }
  /**返回进度信息 */
  @bindThis
  async *progress(options?: { signal?: AbortSignal }) {
    const jsonlines = await this
      .buildInternalApiRequest("/observeUpdateProgress", {
        search: { mmid: this.mmid },
        base: await BasePlugin.public_url,
      })
      .fetch()
      .jsonlines(Number);
    for await (const progress of streamRead(jsonlines, options)) {
      this.progressNum = progress
      yield progress;
    }
  }
  @bindThis
  async getMMid() {
    return await BasePlugin.public_url
  }
}

export const dwebServiceWorkerPlugin = new DwebServiceWorkerPlugin()
