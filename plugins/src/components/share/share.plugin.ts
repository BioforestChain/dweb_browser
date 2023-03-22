import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { toBase64 } from '../../helper/toBase64.ts';
import type {
  CanShareResult,
  ShareOptions,
  ISharePlugin,
} from "./share.type.ts";
import { Directory } from "../file-system/file-system.type.ts";
import { fileSystemPlugin } from "../file-system/file-system.plugin.ts";

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
  async share(options: ShareOptions): Promise<string> {

    let fileUri = ""
    if (options.files) {
      const file = options.files
      // device shareing
      await fileSystemPlugin.writeFile({
        path: file.name,
        data: await toBase64(file),
        directory: Directory.Cache
      });

      const fileResult = await fileSystemPlugin.getUri({
        directory: Directory.Cache,
        path: file.name
      });
      fileUri = fileResult.uri
    }

    return await this.fetchApi(`/share`, {
      search: {
        dialogTitle: options?.dialogTitle,
        title: options?.title,
        text: options?.text,
        url: options?.url,
        files: fileUri,
      },
    }).text();
  }
}




export const sharePlugin = new SharePlugin();
