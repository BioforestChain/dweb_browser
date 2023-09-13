import { IpcHeaders } from "../../../common/deps.ts";
import { createMockModuleServerIpc } from "../../../common/websocketIpc.ts";
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

  readonly ipcPromise = this.createIpc();
  private async createIpc() {
    let pub_url = await BasePlugin.public_url;
    pub_url = pub_url.replace("X-Dweb-Host=api", "X-Dweb-Host=external");
    const url = new URL(pub_url.replace(/^http:/, "ws:"));

    const mmid = url.searchParams.get("X-Dweb-Host")?.slice(9, -4) as $MMID;
    const hash = await BasePlugin.external_url;
    url.pathname = `/${hash}`;
    const ipc = await createMockModuleServerIpc(url, {
      mmid: mmid,
      ipc_support_protocols: {
        cbor: false,
        protobuf: false,
        raw: false,
      },
      dweb_deeplinks: [],
      categories: [],
      name: mmid,
    });
    return ipc;
  }

  // 我前端 ->

  /**拿到更新句柄 */
  @bindThis
  update(): UpdateControllerPlugin {
    return this.updateController;
  }

  /**
   * 关闭前端
   * @returns 
   */
  @bindThis
  close() {
    return this.fetchApi("/close").boolean();
  }

  /**重启后前端 */
  @bindThis
  restart() {
    return this.fetchApi("/restart").boolean();
  }

  /**
   * 查看应用是否安装
   * @param mmid
   */
  @bindThis
  async canOpenUrl(mmid: $MMID) {
    return this.fetchApi(`/check`, {
      search: {
        mmid: mmid,
      },
    }).object<$ExterResponse>();
  }

  /**
   * 跟外部app通信
   * @param pathname
   * @param init
   * @returns
   * https://desktop.dweb.waterbang.top.dweb/say/hi?message="hi 今晚吃螃🦀️蟹吗？"
   */
  @bindThis
  async externalFetch(mmid: $MMID, input: RequestInfo | URL, init?: RequestInit | undefined) {
    const request = new Request(input, { ...init, headers: new IpcHeaders(init?.headers).init("mmid", mmid) });
    const ipc = await this.ipcPromise;
    const ipcResponse = await ipc.request(request.url, request);
    return ipcResponse.toResponse();
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
