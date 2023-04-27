import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import type {
  CanShareResult,
  ShareOptions,
  ISharePlugin,
} from "./share.type.ts";
export class SharePlugin extends BasePlugin implements ISharePlugin {
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
  async canShare(): Promise<CanShareResult> {
    if (typeof navigator === "undefined" || !navigator.share) {
      return { value: false };
    } else {
      return { value: true };
    }
  }

  /**
   * 分享
   * @param options
   * @returns
   */
  @bindThis
  async share(options: ShareOptions): Promise<string> {
    const data = new FormData();
    if (options.files && options.files.length !== 0) {
      for (const key in options.files) {
        const file = options.files[key];
        console.log("fileName=>", file.name);
        data.append("files", file);
      }
    }

    return await this.buildApiRequest("/share", {
      search: {
        title: options?.title,
        text: options?.text,
        url: options?.url,
      },
      method: "POST",
      body: data,
      base: await BasePlugin.public_url.promise,
    })
      .fetch()
      .text();
  }
}

export const sharePlugin = new SharePlugin();
