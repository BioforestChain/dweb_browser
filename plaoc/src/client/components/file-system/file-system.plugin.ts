import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import type {
  FilesOption,
  FilesResult,
  GetUriOptions,
  GetUriResult,
  WriteFileOptions,
  WriteFileResult,
} from "./file-system.type.ts";

export class FileSystemPlugin extends BasePlugin {
  constructor() {
    super("file.sys.dweb");
  }

  @bindThis
  async writeFile(options: WriteFileOptions): Promise<WriteFileResult> {
    const result = await this.fetchApi("/writeFile", {
      search: options,
    }).text();
    return {
      uri: result,
    };
  }

  /**
   * 保存图片到相册
   * @compatibility android/ios only
   * @param options
   * @returns
   */
  @bindThis
  async savePictures(options: FilesOption) {
    const data = new FormData();
    if (options.file) {
      data.append("files", options.file);
    }
    if (options.files && options.files.length !== 0) {
      for (let i = 0; i < options.files.length; i++) {
        const file = options.files.item(i)!;
        data.append("files", file);
      }
    }
    return this.buildApiRequest("/savePictures", {
      method: "POST",
      body: data,
      base: await BasePlugin.public_url,
    })
      .fetch()
      .object<FilesResult>();
  }

  @bindThis
  async getUri(options: GetUriOptions): Promise<GetUriResult> {
    const result = await this.fetchApi("/getUri", {
      search: options,
    });
    return {
      uri: await result.json(),
    };
  }
}

export const fileSystemPlugin = new FileSystemPlugin();
