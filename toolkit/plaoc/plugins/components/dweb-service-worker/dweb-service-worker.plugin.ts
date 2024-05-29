import type { Ipc } from "@dweb-browser/core/ipc/index.ts";
import { createMockModuleServerIpc } from "../../common/websocketIpc.ts";
import { bindThis } from "../../helper/bindThis.ts";
import { buildSearch } from "../../helper/request.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import type { $BuildRequestWithBaseInit } from "../base/base.type.ts";
import type { $DwebRquestInit } from "./dweb-service-worker.type.ts";

/**这是app之间通信的组件 */
export class DwebServiceWorkerPlugin extends BasePlugin {
  readonly tagName = "dweb-service-worker";

  constructor() {
    super("dns.std.dweb");
  }

  readonly ipcPromise: Promise<Ipc> = this.createIpc();
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
        json: false,
      },
      dweb_deeplinks: [],
      categories: [],
      name: mmid,
    });
    return ipc;
  }

  /**
   * 关闭自己的前端
   * @returns boolean
   */
  @bindThis
  close() {
    return this.fetchApi("/close").boolean();
  }

  /**
   * 重启自己的后前端
   * @returns boolean
   */
  @bindThis
  restart() {
    return this.fetchApi("/restart").boolean();
  }

  /**
   * 查询应用是否安装
   * @param mmid
   * @returns boolean
   */
  @bindThis
  has(mmid: $MMID) {
    return this.fetchApi(`/query`, {
      search: {
        mmid: mmid,
      },
    }).boolean();
  }

  /**
   * 向别的app发送request消息
   * @param pathname
   * @param init
   * @returns Promise<Response>
   * @example file://desktop.dweb.waterbang.top.dweb/say/hi?message="hi 今晚吃螃🦀️蟹吗？"
   */
  @bindThis
  async fetch(url: string, init?: $DwebRquestInit | undefined): Promise<Response> {
    const ipc = await this.ipcPromise;
    const input = new URL(url);
    buildSearch(init?.search, (key, value) => {
      input.searchParams.append(key, value);
    });
    if(![...input.searchParams.keys()].includes("activate")) {
      input.searchParams.append("activate", String(!!init?.activate));
    }
    const ipcResponse = await ipc.request(input.href, init);
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
