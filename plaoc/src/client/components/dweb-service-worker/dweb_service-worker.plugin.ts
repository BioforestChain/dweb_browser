import { bindThis } from "../../helper/bindThis.ts";
import type { $BuildRequestWithBaseInit } from "../base/BasePlugin.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { configPlugin } from "../index.ts";
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

  /**关闭前端 */
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
  async externalFetch(mmid: $MMID, init: $ExterRequestWithBaseInit) {
    // http://localhost:22206/?X-Dweb-Host=api.desktop.dweb.waterbang.top.dweb%3A443
    let pub = await BasePlugin.public_url;
    if (pub === "") {
      pub = await configPlugin.updatePublicUrl();
    }
    pub = pub.replace("X-Dweb-Host=api", "X-Dweb-Host=external");
    const X_Plaoc_Public_Url = new URL(location.href).searchParams.get(
      "X-Plaoc-External-Url"
    );

    const search = Object.assign(init.search ?? {}, {
      mmid: mmid,
      action: "request",
      pathname: init.pathname,
    });
    const config = Object.assign(init, { search: search, base: pub });
    return await this.buildExternalApiRequest(
      `/${X_Plaoc_Public_Url}`,
      config
    ).fetch();
  }
  // http://localhost:22206/?X-Dweb-Host=external.demo.www.bfmeta.info.dweb%3A443
}

export type $MMID = `${string}.dweb`;

export interface $ExterRequestWithBaseInit extends $BuildRequestWithBaseInit {
  pathname: string;
}

export const dwebServiceWorkerPlugin = new DwebServiceWorkerPlugin();
