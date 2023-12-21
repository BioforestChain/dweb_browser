import { encode } from "cbor-x";
import { PromiseOut } from "../../helper/PromiseOut.ts";
import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { FileData, FileDataEncode, ShareResult, type ShareOptions } from "./share.type.ts";
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

  // /**
  //  * 分享
  //  * @param options
  //  * @returns
  //  */
  // @bindThis
  // async share(options: ShareOptions): Promise<ShareResult> {
  //   const data = new FormData();
  //   if (options.files && options.files.length !== 0) {
  //     for (let i = 0; i < options.files.length; i++) {
  //       const file = options.files.item(i)!;
  //       data.append("files", file);
  //     }
  //   }

  //   if(options.file) {
  //     data.append("files", options.file)
  //   }

  //   const result = await this.buildApiRequest("/share", {
  //     search: {
  //       title: options?.title,
  //       text: options?.text,
  //       url: options?.url,
  //     },
  //     method: "POST",
  //     body: data,
  //   })
  //     .fetch()
  //     .object<ShareResult>();
  //   return result;
  // }

  #MAX_SIZE = 500 * 1024;
  async #checkSize(width: number, height: number, img: HTMLImageElement): Promise<[number, number]> {
    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext("2d");
    ctx?.drawImage(img, 0, 0, width, height);
    const dataUrl = canvas.toDataURL("image/jpeg");
    const size = atob(dataUrl.split(",")[1]).length;
    return size > this.#MAX_SIZE ? this.#checkSize(width * 0.8, height * 0.8, img) : [width, height];
  }

  async #fileToBase64String(file: File): Promise<string> {
    const reader = new FileReader();
    const po = new PromiseOut<string>();

    reader.onload = (ev) => {
      if (file.type.startsWith("image/")) {
        const img = new Image();
        img.onload = async () => {
          let width = img.width, height = img.height;
          if(file.size > this.#MAX_SIZE) {
            [width, height] = await this.#checkSize(img.width, img.height, img);
          }
          
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
            "image/jpeg",
            0.7
          );
        };
        img.src = ev.target!.result as string;
      }
    };

    reader.readAsDataURL(file);

    return await po.promise;
  }

  /**
   * 分享
   * @param options
   * @returns
   */
  @bindThis
  async share(options: ShareOptions): Promise<ShareResult> {
    const fileList: FileData[] = [];
    if (options.files && options.files.length !== 0) {
      for (let i = 0; i < options.files.length; i++) {
        const file = options.files.item(i)!;
        const data = await this.#fileToBase64String(file);

        fileList.push({
          name: file.name,
          type: file.type,
          size: file.size,
          encode: FileDataEncode.BASE64,
          data,
        });
      }
    }

    if (options.file) {
      const data = await this.#fileToBase64String(options.file);

      fileList.push({
        name: options.file.name,
        type: options.file.type,
        size: options.file.size,
        encode: FileDataEncode.BASE64,
        data,
      });
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
      .object<ShareResult>();
    return result;
  }
}

export const sharePlugin = new SharePlugin();
