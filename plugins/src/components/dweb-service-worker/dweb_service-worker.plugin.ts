import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { BFSMetaData } from "./dweb-service-worker.type.ts";

class UpdateControllerPlugin extends BasePlugin {
  tagName = "dweb-update-controller";

  progressNum = 0;

  constructor() {
    super("jmm.sys.dweb")
  }

  async init() {
  }

  /**下载 */
  @bindThis
  async download(metadataUrl: string): Promise<BFSMetaData> {
    return await this.fetchApi(`/install`, {
      search: {
        metadataUrl
      }
    }).object()
  }

  // 暂停
  @bindThis
  async pause(): Promise<boolean> {
    return await this.fetchApi("/pause").boolean()
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
  // @bindThis
  // async *progress(options?: { signal?: AbortSignal }) {
  //   const jsonlines = await this
  //     .buildInternalApiRequest("/observeUpdateProgress", {
  //       search: { mmid: this.mmid },
  //       base: await BasePlugin.public_url,
  //     })
  //     .fetch()
  //     .jsonlines(Number);
  //   for await (const progress of streamRead(jsonlines, options)) {
  //     this.progressNum = progress
  //     yield progress;
  //   }
  // }
  @bindThis
  async getMMid() {
    return await BasePlugin.public_url
  }
}

export class DwebServiceWorkerPlugin extends BasePlugin {
  tagName = "dweb-service-worker";

  updateController = new UpdateControllerPlugin()

  constructor() {
    super("dns.sys.dweb")
  }
  /**拿到更新句柄 */
  @bindThis
  update(): UpdateControllerPlugin {
    return this.updateController
  }

  /**关闭前后端 */
  @bindThis
  async close(): Promise<boolean> {
    return await this.fetchApi("/close").boolean()
  }

  /**重启后前端 */
  @bindThis
  async restart(): Promise<boolean> {
    return await this.fetchApi("/restart").boolean()
  }
}

export const dwebServiceWorkerPlugin = new DwebServiceWorkerPlugin()
