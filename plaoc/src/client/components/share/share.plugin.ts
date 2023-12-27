import { encode } from "cbor-x";
import { PromiseOut } from "../../helper/PromiseOut.ts";
import { bindThis } from "../../helper/bindThis.ts";
import { FileData, FileDataEncode, normalToBase64String } from "../../util/file.ts";
import { BaseResult } from "../../util/response.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { type ImageBlobOptions, type ShareOptions } from "./share.type.ts";
export class SharePlugin extends BasePlugin {
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
  async #multipartShare(options: ShareOptions): Promise<BaseResult> {
    const data = new FormData();
    if (options.files && options.files.length !== 0) {
      for (let i = 0; i < options.files.length; i++) {
        const file = options.files.item(i)!;
        data.append("files", file);
      }
    }

    if (options.file) {
      data.append("files", options.file);
    }

    const result = await this.buildApiRequest("/share", {
      search: {
        title: options?.title,
        text: options?.text,
        url: options?.url,
      },
      method: "POST",
      body: data,
    })
      .fetch()
      .object<BaseResult>();
    return result;
  }

  async #blobToBase64String(file: File, blobOptions: ImageBlobOptions): Promise<string> {
    const reader = new FileReader();
    const po = new PromiseOut<string>();

    reader.onload = (ev) => {
      const img = new Image();
      img.onload = () => {
        const width = img.width,
          height = img.height;

        const canvas = document.createElement("canvas");
        const ctx = canvas.getContext("2d");
        canvas.width = width;
        canvas.height = height;
        ctx?.drawImage(img, 0, 0, width, height);

        canvas.toBlob(
          (blob) => {
            const imgReader = new FileReader();

            imgReader.onloadend = () => {
              let binary = "";
              const bytes = new Uint8Array(imgReader.result as ArrayBuffer);
              for (const byte of bytes) {
                binary += String.fromCharCode(byte);
              }
              po.resolve(btoa(binary));
            };

            imgReader.readAsArrayBuffer(blob!);
          },
          blobOptions.type.startsWith("image/") ? blobOptions.type : "image/jpeg",
          blobOptions.quality > 0 && blobOptions.quality <= 1 ? blobOptions.quality : 0.8
        );
      };
      img.src = ev.target!.result as string;
    };

    reader.readAsDataURL(file);

    return await po.promise;
  }

  async #cborNormalShareFileList(options: ShareOptions): Promise<FileData[]> {
    const fileList: FileData[] = [];
    if (options.files && options.files.length !== 0) {
      for (let i = 0; i < options.files.length; i++) {
        const file = options.files.item(i)!;
        const data = await normalToBase64String(file);

        fileList.push({
          name: file.name,
          type: file.type,
          size: file.size,
          encoding: FileDataEncode.BASE64,
          data,
        });
      }
    }

    if (options.file) {
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

  async #cborCanvasCompressShareFileList(options: ShareOptions): Promise<FileData[]> {
    const fileList: FileData[] = [];
    if (options.files && options.files.length !== 0) {
      for (let i = 0; i < options.files.length; i++) {
        const file = options.files.item(i)!;
        const data = await this.#blobToBase64String(file, options.imageBlobOptions!);

        fileList.push({
          name: file.name,
          type: file.type,
          size: file.size,
          encoding: FileDataEncode.BASE64,
          data,
        });
      }
    }

    if (options.file) {
      const data = await this.#blobToBase64String(options.file, options.imageBlobOptions!);

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

  async #cborImageListPost(options: ShareOptions): Promise<BaseResult> {
    let fileList: FileData[] = [];
    if (options.imageBlobOptions) {
      fileList = await this.#cborCanvasCompressShareFileList(options);
    } else {
      fileList = await this.#cborNormalShareFileList(options);
    }

    const shareBody = encode(fileList);

    const result = await this.buildApiRequest("/share", {
      search: {
        title: options?.title,
        text: options?.text,
        url: options?.url,
      },
      headers: {
        "Content-Type": "application/cbor",
        "Content-Length": shareBody.length,
      },
      method: "POST",
      body: shareBody,
    })
      .fetch()
      .object<BaseResult>();
    return result;
  }

  /**
   * 分享
   * @param options
   * @returns
   */
  @bindThis
  async share(options: ShareOptions): Promise<BaseResult> {
    if (
      (options.file && options.file.type.startsWith("image/")) ||
      (options.files && options.files[0].type.startsWith("image/"))
    ) {
      return await this.#cborImageListPost(options);
    }

    return await this.#multipartShare(options);
  }
}

export const sharePlugin = new SharePlugin();
