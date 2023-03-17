import { BasePlugin } from "../basePlugin.ts";
import {
  SupportedFormat,
} from "./barcode-scanning.type.ts";

export class BarcodeScannerPlugin extends BasePlugin {
  readonly tagName = "dweb-barcode-scanning";

  // private _video: HTMLVideoElement | null = null;
  // private _options: ScanOptions | null = null;
  // private _backgroundColor: string | null = null;
  // private _promiseOutR = new PromiseOut<Response>();

  constructor() {
    super("barcode-scanning.sys.dweb");
  }

  /**
   *  识别二维码
   * @param blob 
   * @param rotation 
   * @param formats 
   * @returns 
   */
  async process(blob: Blob, rotation = 0, formats = SupportedFormat.QR_CODE) {
    return await this.buildApiRequest("/process", {
      search: {
        rotation,
        formats,
      },
      method: "POST",
      body: blob,
      base: await BasePlugin.public_url,
    }).fetch()
  }
  /**
   * 停止扫码
   * @returns 
   */
  async stop() {
    return await this.fetchApi(`/stop`)
  }

  // /**
  //  *  准备扫描
  //  * @param targetedFormats 扫描文本类型
  //  * @param cameraDirection 摄像头位置
  //  */
  // async prepare(
  //   options?: ScanOptions
  // ): Promise<void> {
  //   this._direction = options?.cameraDirection ?? CameraDirection.BACK
  //   this._formats = options?.targetedFormats ?? [SupportedFormat.QR_CODE]
  //   await this._getVideoElement();
  //   return;
  // }

  // /**
  //  *  开始扫描
  //  * @param targetedFormats 扫描文本类型
  //  * @param cameraDirection 摄像头位置
  //  */
  // async startScan(
  //   options?: ScanOptions
  // ): Promise<ScanResult> {
  //   this._direction = options?.cameraDirection ?? CameraDirection.BACK
  //   this._formats = options?.targetedFormats ?? [SupportedFormat.QR_CODE]
  //   const video = await this._getVideoElement();
  //   if (video) {
  //     await this._getFirstResultFromReader();
  //   } else {
  //     throw Error("Missing video element");
  //   }
  //   return {
  //     hasContent: true,
  //     content: "",
  //     format: "QR_CODE",
  //   }
  // }

  // /**
  //  * 暂停扫描
  //  */
  // // deno-lint-ignore require-await
  // async pauseScanning() {
  //   if (!this._promiseOutR.is_finished) {
  //     this._promiseOutR.resolve(new Response());
  //   }
  //   await this.nativeFetch(`/stop`);
  // }

  // /**
  //  * 恢复扫描
  //  */
  // async resumeScanning(): Promise<void> {
  //   await this._getFirstResultFromReader();
  // }

  // /**
  //  *  停止扫描
  //  * @param forceStopScan 是否强制停止扫描
  //  */
  // // deno-lint-ignore no-unused-vars
  // async stopScan(options?: StopScanOptions): Promise<void> {
  //   this._stop();
  //   await this.stop()
  // }

  // /**
  //  *  检查是否有摄像头权限，如果没有或者被拒绝，那么会强制请求打开权限(设置)
  //  * @param forceCheck 是否强制检查权限
  //  */
  // async checkCameraPermission(
  //   _forceCheck?: boolean,
  //   _beforeOpenPermissionSettings?: () => boolean | Promise<boolean>
  // ) {
  //   if (typeof navigator === "undefined" || !navigator.permissions) {
  //     throw Error("Permissions API not available in this browser");
  //   }
  //   try {
  //     // https://developer.mozilla.org/en-US/docs/Web/API/Permissions/query
  //     // 所支持的特定权限因实现该功能的浏览器而异
  //     // 权限 API，所以我们需要一个 try/catch 以防 'camera' 无效
  //     const permission = await window.navigator.permissions.query({
  //       // deno-lint-ignore no-explicit-any
  //       name: "camera" as any,
  //     });
  //     if (permission.state === "prompt") {
  //       return {
  //         neverAsked: true,
  //       };
  //     }
  //     if (permission.state === "denied") {
  //       return {
  //         denied: true,
  //       };
  //     }
  //     if (permission.state === "granted") {
  //       return {
  //         granted: true,
  //       };
  //     }
  //     return {
  //       unknown: true,
  //     };
  //   } catch {
  //     throw Error("Camera permissions are not available in this browser");
  //   }
  // }
  // /**
  //  * 隐藏webview背景
  //  */
  // // deno-lint-ignore require-await
  // async hideBackground(): Promise<void> {
  //   this._backgroundColor = document.documentElement.style.backgroundColor;
  //   document.documentElement.style.backgroundColor = "transparent";
  //   return;
  // }

  // /**
  //  * 显示webview背景
  //  */
  // // deno-lint-ignore require-await
  // async showBackground(): Promise<void> {
  //   document.documentElement.style.backgroundColor =
  //     this._backgroundColor || "";
  //   return;
  // }

  // private async _getVideoElement() {
  //   if (!this._video) {
  //     await this._startVideo();
  //   }
  //   return this._video;
  // }
  // /**
  //  * 返回扫码完的结果
  //  * @returns
  //  */
  // private async _getFirstResultFromReader() {
  //   this._promiseOutR = new PromiseOut();
  //   const videoElement = await this._getVideoElement();
  //   if (videoElement) {
  //     await this._promiseOutR.promise;
  //   }
  // }
  // private async processPhoto(blob: Blob) {
  //   await this.nativeFetch(`/process?rotation=${0}&formats=${this._formats}`, {
  //     method: "POST",
  //     body: blob,
  //   })
  //     .then((res) => {
  //       this._promiseOutR.resolve(res);
  //     })
  //     .catch((err) => {
  //       this._promiseOutR.reject(err);
  //     });
  // }

  // // deno-lint-ignore no-explicit-any
  // private stopCamera(error: any) {
  //   console.error(error);
  //   this._stop(); // turn off the camera
  // }

  // // deno-lint-ignore no-explicit-any
  // private async _stop(): Promise<any> {
  //   if (this._video) {
  //     this._video.pause();

  //     // deno-lint-ignore no-explicit-any
  //     const st: any = this._video.srcObject;
  //     const tracks = st.getTracks();

  //     for (let i = 0; i < tracks.length; i++) {
  //       const track = tracks[i];
  //       track.stop();
  //     }
  //     await this._video.parentElement?.remove();
  //     this._video = null;
  //   }
  // }
}

export const barcodeScannerPlugin = new BarcodeScannerPlugin();
