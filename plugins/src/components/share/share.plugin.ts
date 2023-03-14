import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../basePlugin.ts";
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
   * 判断是否能分享
   * web Only
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
  async share(options: ShareOptions): Promise<Response> {
    return await this.fetchApi(`/share`, {
      search: {
        dialogTitle: options?.dialogTitle,
        title: options?.title,
        text: options?.text,
        url: options?.url,
        files: options?.files,
      },
    });
  }
}
export const sharePlugin = new SharePlugin();
