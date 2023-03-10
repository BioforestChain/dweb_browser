import { BasePlugin } from "../basePlugin.ts";
import { CanShareResult, ShareOptions, ISharePlugin } from "./share.type.ts";

export class SharePlugin extends BasePlugin implements ISharePlugin {


  constructor(readonly mmid = "share.sys.dweb") {
    super(mmid, "Share")
  }

  /**
   * 判断是否能分享 
   * web Only
   * @returns 
   */
  // deno-lint-ignore require-await
  async canShare(): Promise<CanShareResult> {
    if (typeof navigator === 'undefined' || !navigator.share) {
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
  async share(options: ShareOptions): Promise<Response> {
    return await this.nativeFetch(`/share`, {
      search: {
        dialogTitle: options?.dialogTitle,
        title: options?.title,
        text: options?.text,
        url: options?.url,
        files: options?.files
      }
    })
  }
}
