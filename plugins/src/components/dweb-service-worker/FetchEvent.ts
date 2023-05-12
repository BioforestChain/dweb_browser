import { $BuildRequestWithBaseInit } from "../base/BasePlugin.ts";
import { dwebServiceWorkerPlugin } from "./dweb_service-worker.plugin.ts";

interface FetchEventInit {
  request: Request;
  clientId?: string;
}

export type $FetchEventType = "fetch" | "onFetch";
// | "activate"
// | "install"
// | "message";

export class FetchEvent extends Event {
  plugin = dwebServiceWorkerPlugin;
  readonly type: $FetchEventType;
  request: Request;
  clientId: string | null;
  // isReload?: boolean; // 弃用
  // resultingClientId: string | null; // 不需要
  // deno-lint-ignore no-explicit-any
  waitUntilPromise: Promise<any> | any;

  constructor(type: $FetchEventType, init: FetchEventInit) {
    super(type);
    this.type = type;
    this.request = init.request;
    this.clientId = init.clientId || null;

    // this.isReload = init.isReload || false;
    // this.resultingClientId = null;
    this.waitUntilPromise = null;
  }
  
  async fetch(pathname: string, init?: $BuildRequestWithBaseInit) {
    return await this.plugin.buildExternalApiRequest(pathname, init).fetch();
  }

  respondWith(response: Response | Promise<Response>) {
    if (!(response instanceof Response)) {
      response = Promise.resolve(response).then((res) => {
        if (!(res instanceof Response)) {
          throw new TypeError(
            "The value returned from respondWith() must be a Response or a Promise that resolves to a Response."
          );
        }
        return res;
      });
    }
    this.waitUntilPromise = response;
  }
  /**
   * 将Promise添加到事件的等待列表中。
   * 这些Promise对象将在Service Worker的生命周期内持续运行，直到它们全部解决或被拒绝。
   * @param promise
   */
  // deno-lint-ignore no-explicit-any
  waitUntil(promise: Promise<any>) {
    if (!this.waitUntilPromise) {
      this.waitUntilPromise = Promise.resolve();
    }
    this.waitUntilPromise = this.waitUntilPromise.then(() => promise);
  }
}

// deno-lint-ignore no-explicit-any
if (typeof (globalThis as any)["FetchEvent"] === "undefined") {
  Object.assign(globalThis, { FetchEvent });
}
