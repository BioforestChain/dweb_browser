import { $BuildRequestWithBaseInit, BasePlugin } from "../base/BasePlugin.ts";
import { dwebServiceWorkerPlugin } from "./dweb_service-worker.plugin.ts";

interface FetchEventInit {
  request: Request;
  clientId?: string;
}

export type $FetchEventType = "fetch";
//| "onFetch"
// | "activate"
// | "install"
// | "message";

export class FetchEvent extends Event {
  plugin = dwebServiceWorkerPlugin;
  request: Request;
  clientId: string | null;
  public_url = BasePlugin.public_url;

  constructor(type: $FetchEventType, init: FetchEventInit) {
    super(type);
    this.request = init.request;
    this.clientId = init.clientId || null;
  }
  // 回复别的app的消息
  private async fetch(pathname: string, init?: $BuildRequestWithBaseInit) {
    return await this.plugin.buildExternalApiRequest(pathname, init).fetch();
  }

  async respondWith(response: Blob | ReadableStream<Uint8Array> | string) {
    if (!this.public_url)
      throw Error("you need init <web-config></dweb-config>");

    const public_url = new URL(await BasePlugin.public_url);
    const base = public_url.href.replace(
      /X-Dweb-Host=api/,
      "X-Dweb-Host=external"
    );
    const X_Plaoc_Public_Url = new URL(location.href).searchParams.get("X-Plaoc-External-Url")
    return this.fetch(`/${X_Plaoc_Public_Url}`, {
      method: "POST",
      search: {
        id: this.clientId,
        action:"response"
      },
      body: response,
      base: base,
    });
  }
}

// deno-lint-ignore no-explicit-any
if (typeof (globalThis as any)["FetchEvent"] === "undefined") {
  Object.assign(globalThis, { FetchEvent });
}
