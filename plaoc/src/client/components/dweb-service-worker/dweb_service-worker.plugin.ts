import { bindThis } from "../../helper/bindThis.ts";
import type { $BuildRequestWithBaseInit } from "../base/BasePlugin.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { BFSMetaData } from "./dweb-service-worker.type.ts";

class UpdateControllerPlugin extends BasePlugin {
  readonly tagName = "dweb-update-controller";

  progressNum = 0;

  constructor() {
    super("jmm.browser.dweb");
  }

  async init() {}

  /**下载 */
  @bindThis
  async download(url: string): Promise<BFSMetaData> {
    return await this.fetchApi(`/install`, {
      search: {
        url,
      },
    }).object();
  }

  // 暂停
  @bindThis
  async pause(): Promise<boolean> {
    return await this.fetchApi("/pause").boolean();
  }
  // 重下
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

export class DwebServiceWorkerPlugin extends BasePlugin {
  readonly tagName = "dweb-service-worker";

  updateController = new UpdateControllerPlugin();

  constructor() {
    super("dns.sys.dweb");
  }
  /**拿到更新句柄 */
  @bindThis
  update(): UpdateControllerPlugin {
    return this.updateController;
  }

  /**关闭前后端 */
  @bindThis
  async close(): Promise<boolean> {
    return await this.fetchApi("/close").boolean();
  }

  /**重启后前端 */
  @bindThis
  async restart(): Promise<boolean> {
    return await this.fetchApi("/restart").boolean();
  }

  /**
   * 跟外部app通信
   * @param pathname
   * @param init
   * @returns
   * https://desktop.dweb.waterbang.top.dweb/say/hi?message="hi 今晚吃螃🦀️蟹吗？"
   */
  @bindThis
  async externalFetch(hostname: $MMID, init: $ExterRequestWithBaseInit) {
    // http://localhost:22206/?X-Dweb-Host=api.desktop.dweb.waterbang.top.dweb%3A443
    const public_url = new URL(await BasePlugin.public_url);
    public_url.searchParams.set("X-Dweb-Host", `external.${hostname}:443`);
    const base = public_url.href;

    const config = Object.assign(init ?? {}, { base: base });
    return await this.buildExternalApiRequest(init.pathname, config).fetch();
  }
  // http://localhost:22206/?X-Dweb-Host=external.demo.www.bfmeta.info.dweb%3A443
}

export type $MMID = `${string}.dweb`;

export interface $ExterRequestWithBaseInit extends $BuildRequestWithBaseInit {
  pathname: string;
}

export const dwebServiceWorkerPlugin = new DwebServiceWorkerPlugin();
