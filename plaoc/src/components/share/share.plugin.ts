import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import type { ShareOptions, ShareResult } from "./share.type.ts";
export class SharePlugin extends BasePlugin {
  readonly tagName = "dweb-share";

  constructor() {
    super("share.sys.dweb");
  }

  /**
   *(desktop Only)
   * 判断是否能分享
   * @returns
   */
  @bindThis
  // deno-lint-ignore require-await
  async canShare(): Promise<boolean> {
    if (typeof navigator === "undefined" || !navigator.share) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * 分享
   * @param options
   * @returns
   */
  @bindThis
  async share(options: ShareOptions): Promise<ShareResult> {
    const data = new FormData();
    if (options.files && options.files.length !== 0) {
      for (const key in options.files) {
        const file = options.files[key];
        data.append("files", file);
      }
    }
    const result = await this.buildApiRequest("/share", {
      search: {
        title: options?.title,
        text: options?.text,
        url: options?.url,
      },
      method: "POST",
      body: data,
      base:await BasePlugin.public_url,
    })
      .fetch()
      .object<ShareResult>();
    return result;
  }
}

export const sharePlugin = new SharePlugin();
