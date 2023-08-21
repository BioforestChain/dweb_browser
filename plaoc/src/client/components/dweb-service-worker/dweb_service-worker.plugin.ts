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
  async download(metadataUrl: string): Promise<BFSMetaData> {
    return await this.fetchApi(`/install`, {
      search: {
        metadataUrl,
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
    super("dns.std.dweb");
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
  async externalFetch(mmid: $MMID, init: $ExterRequestWithBaseInit): Promise<$ExternalFetchHandle> {
    let pub = await BasePlugin.public_url;
    pub = pub.replace("X-Dweb-Host=api", "X-Dweb-Host=external");
    const X_Plaoc_Public_Url = await BasePlugin.external_url;
    // const controller = new AbortController();
    const search = Object.assign(init.search ?? {}, {
      mmid: mmid,
      action: "request",
      pathname: init.pathname,
    });
    const config = Object.assign(init, { search: search, base: pub });
    return {
      response: this.buildExternalApiRequest(`/${X_Plaoc_Public_Url}`, config).fetch(),
      close: this.externalClose.bind(this, mmid),
    };
  }

  /**
   * 关闭连接
   */
  @bindThis
  async externalClose(mmid: $MMID): Promise<$ExterResponse> {
    let pub = await BasePlugin.public_url;
    pub = pub.replace("X-Dweb-Host=api", "X-Dweb-Host=external");
    const X_Plaoc_Public_Url = await BasePlugin.external_url;
    return this.buildExternalApiRequest(`/${X_Plaoc_Public_Url}`, {
      search: {
        mmid: mmid,
        action: "close",
      },
      base: pub,
    })
      .fetch()
      .object<$ExterResponse>();
  }

  /**
   * 查看对方是否监听了请求
   * @param mmid
   */
  @bindThis
  async ping(mmid: $MMID): Promise<boolean> {
    let pub = await BasePlugin.public_url;
    pub = pub.replace("X-Dweb-Host=api", "X-Dweb-Host=external");
    const X_Plaoc_Public_Url = await BasePlugin.external_url;
    return this.buildExternalApiRequest(`/${X_Plaoc_Public_Url}`, {
      search: {
        mmid: mmid,
        action: "ping",
      },
      base: pub,
    })
      .fetch()
      .boolean();
  }
}

export type $MMID = `${string}.dweb`;

export interface $ExterRequestWithBaseInit extends $BuildRequestWithBaseInit {
  pathname: string;
}

export interface $ExternalFetchHandle {
  close: () => void;
  response: Promise<Response>;
}

export interface $ExterResponse {
  success: boolean;
  message: string;
}

export const dwebServiceWorkerPlugin = new DwebServiceWorkerPlugin();
