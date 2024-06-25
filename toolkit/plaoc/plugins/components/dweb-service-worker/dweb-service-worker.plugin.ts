import { WebSocketIpcBuilder } from "../../common/websocketIpc.ts";
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

  readonly ipc = this.createIpc();

  private createIpc() {
    const api_url = BasePlugin.api_url.replace("://api", "://external");
    const url = new URL(api_url.replace(/^http/, "ws"));
    const mmid = location.host.slice(9) as $MMID;
    const hash = BasePlugin.external_url;
    url.pathname = `/${hash}`;
    const wsIpcBuilder = new WebSocketIpcBuilder(url, {
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
    return wsIpcBuilder.ipc;
  }

  /**
   * 关闭app
   * @returns boolean
   */
  @bindThis
  close() {
    return this.fetchApi("/close").boolean();
  }

  /**
   * 重启app
   * @returns boolean
   */
  @bindThis
  restart() {
    return this.fetchApi("/restart").boolean();
  }

  /**
   * 配置没有命中缓存是否自动清空数据
   * @returns boolean
   */
  @bindThis
  async clearCache() {
    const KEY = "--plaoc-session-id--";
    const sessionId = localStorage.getItem(KEY);
    localStorage.clear();
    if (sessionId) localStorage.setItem(KEY, sessionId);
    sessionStorage.clear();
    const tasks = [];
    const t1 = indexedDB.databases().then((dbs) => {
      for (const db of dbs) {
        if (db.name) {
          indexedDB.deleteDatabase(db.name);
        }
      }
    });
    tasks.push(t1.catch(console.error));
    // @ts-ignore
    if (typeof cookieStore === "object") {
      // @ts-ignore
      const t2 = cookieStore.getAll().then((cookies) => {
        for (const c of cookies) {
          // @ts-ignore
          cookieStore.delete(c.name);
        }
      });
      tasks.push(t2.catch(console.error));
    }
    await Promise.all(tasks);
    (location as Location).replace(location.href);
  }

  /**
   * 查询应用是否安装
   * @param mmid
   * @returns boolean
   */
  @bindThis
  async has(mmid: $MMID) {
    const res = await this.fetchApi(`/query`, {
      search: {
        mmid: mmid,
      },
    });

    return res.status === 200;
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
    const input = new URL(url);
    buildSearch(init?.search, (key, value) => {
      input.searchParams.append(key, value);
    });
    if (![...input.searchParams.keys()].includes("activate")) {
      input.searchParams.append("activate", String(!!init?.activate));
    }
    const ipcResponse = await this.ipc.request(input.href, init);
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
