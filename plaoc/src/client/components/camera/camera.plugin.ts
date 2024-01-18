import { bindThis } from "../../helper/bindThis.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import type { ImageOptions, Photo } from "./camera.type.ts";

export class CameraPlugin extends BasePlugin {
  readonly tagName = "dweb-camera";

  constructor() {
    super("camera.sys.dweb");
  }
  /**
   * 向系统获取图片
   * （如果没有传递任何参数，会默认询问用户打开相册或者拍照）
   * @param options
   * @returns Photo
   */
  @bindThis
  async getPhoto(options?: ImageOptions) {
    return await this.fetchApi("/getPhoto", {
      search: {
        options: options, 
      },
    }).object<Photo>();
  }
}

export const cameraPlugin = new CameraPlugin();
