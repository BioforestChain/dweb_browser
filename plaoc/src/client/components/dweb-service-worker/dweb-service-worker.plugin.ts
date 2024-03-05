import { ReadableStreamIpc } from "dweb/core/index.ts";
import { IpcHeaders } from "dweb/core/ipc/index.ts";
import { createMockModuleServerIpc } from "../../common/websocketIpc.ts";
import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import type { $BuildRequestWithBaseInit } from "../base/base.type.ts";
import { $DwebResult } from "../base/base.type.ts";

export class DwebServiceWorkerPlugin extends BasePlugin {
  readonly tagName = "dweb-service-worker";

  constructor() {
    super("dns.std.dweb");
  }

  readonly ipcPromise: Promise<ReadableStreamIpc> = this.createIpc();
  private async createIpc() {
    const api_url = BasePlugin.api_url.replace("://api", "://external");
    const url = new URL(api_url.replace(/^http/, "ws"));
    const mmid = location.host.slice(9) as $MMID;
    const hash = BasePlugin.external_url;
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
    return this.fetchApi("/restart").object<$DwebResult>();
  }

  /**
   * 查看应用是否安装
   * @param mmid
   */
  @bindThis
  async canOpenUrl(mmid: $MMID): Promise<$DwebResult> {
    try {
      const res = await this.fetchApi(`/query`, {
        search: {
          mmid: mmid,
        },
      });
      if (res.ok && (await res.json())) {
        return { success: true, message: "true" };
      }
      return { success: false, message: "false" };
    } catch (e) {
      return { success: false, message: e };
    }
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

export const dwebServiceWorkerPlugin = new DwebServiceWorkerPlugin();
