import { bindThis } from "../../../helper/bindThis.ts";
import { BasePlugin } from "../../base/BasePlugin.ts";

export class FilePickerPlugin extends BasePlugin {
  constructor() {
    super("file-picker.sys.dweb");
  }

  /**
   * 选择文件
   * @param mime
   * @returns path
   */
  @bindThis
  async capture(mime: string) {
    if (!mime.startsWith("audio") || mime.startsWith("video") || !mime.startsWith("image")) {
      throw Error("currently only supports audio/*, video/*, image/* ！");
    }
    return await this.fetchApi("/capture", {
      search: {
        mime: mime,
      },
    }).text();
  }
}

export const mediaCapturePlugin = new FilePickerPlugin();