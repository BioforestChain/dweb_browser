import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import type { ClipboardWriteOptions, ReadResult } from "./clipboard.type.ts";

export class ClipboardPlugin extends BasePlugin {
  constructor() {
    super("clipboard.sys.dweb");
  }
  /** 读取剪切板 */
  @bindThis
  async read(): Promise<ReadResult> {
    return await this.fetchApi("/read").object<ReadResult>();
  }
  /** 写入剪切板 */
  @bindThis
  async write(options: ClipboardWriteOptions) {
    await this.fetchApi("/write", {
      search: {
        string: options.string,
        url: options.url,
        image: options.image,
        label: options.label,
      },
    });
  }
}

export const clipboardPlugin = new ClipboardPlugin();
