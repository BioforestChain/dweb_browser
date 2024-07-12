import { encode } from "cbor-x";
import { bindThis } from "../../helper/bindThis.ts";
import { type FileData, FileDataEncode, normalToBase64String } from "../../util/file.ts";
import { BasePlugin } from "../base/base.plugin.ts";
import type { MediaOption } from "./media.type.ts";

export class MediaPlugin extends BasePlugin {
  constructor() {
    super("media.file.sys.dweb");
  }

  /**
   * 保存图片到相册
   * @compatibility android/ios only
   * @param options
   * @returns
   */
  @bindThis
  async savePictures(options: MediaOption) {
    const fileList = await this.#encodeFileData(options);

    const shareBody = encode(fileList);

    return this.buildApiRequest("/savePictures", {
      search: {
        saveLocation: options.saveLocation,
      },
      headers: {
        "Content-Type": "application/cbor",
        "Content-Length": shareBody.length.toString(),
      },
      method: "POST",
      body: shareBody,
    }).fetch();
  }

  /**
   * 分享 multipart/form-data 暂未启用
   * @param options
   * @returns
   */
  #multipartSavePictures(options: MediaOption) {
    const data = new FormData();
    // if (options.files && options.files.length !== 0) {
    //   for (let i = 0; i < options.files.length; i++) {
    //     const file = options.files.item(i)!;
    //     data.append("files", file);
    //   }
    // }

    if (options.file) {
      data.append("files", options.file);
    }

    return this.buildApiRequest("/savePictures", {
      search: {
        saveLocation: options.saveLocation,
      },
      method: "POST",
      body: data,
    }).fetch();
  }

  /**
   * 生成fileData
   * @param options
   * @returns
   */
  async #encodeFileData(options: MediaOption): Promise<FileData[]> {
    const fileList: FileData[] = [];

    if (options.file && options.file.type.startsWith("image/")) {
      const data = await normalToBase64String(options.file);

      fileList.push({
        name: options.file.name,
        type: options.file.type,
        size: options.file.size,
        encoding: FileDataEncode.BASE64,
        data,
      });
    }

    return fileList;
  }
}

export const mediaPlugin = new MediaPlugin();
