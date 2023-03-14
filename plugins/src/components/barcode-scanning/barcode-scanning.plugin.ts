/// <reference path="../../shims/ImageCapture.d.ts"/>
import { ImageCapture, PromiseOut } from "../../../deps.ts";
import { BasePlugin } from "../basePlugin.ts";
import {
  CameraDirection,
  ScanOptions,
  SupportedFormat,
} from "./barcode-scanning.type.ts";

export class BarcodeScannerPlugin extends BasePlugin {
  readonly tagName = "dweb-barcode-scanning";
  private _formats = SupportedFormat.QR_CODE;
  private _direction: string = CameraDirection.BACK;
  private _video: HTMLVideoElement | null = null;
  private _options: ScanOptions | null = null;
  private _backgroundColor: string | null = null;
  private _promiseOutR = new PromiseOut<Response>();

  constructor() {
    super("barcode-scanning.sys.dweb");
  }

  /**
   *  准备扫描
   * @param targetedFormats 扫描文本类型
   * @param cameraDirection 摄像头位置
   */
  async prepare(
    targetedFormats: SupportedFormat = SupportedFormat.QR_CODE,
    cameraDirection: CameraDirection = CameraDirection.BACK
  ): Promise<void> {
    this._direction = cameraDirection;
    this._formats = targetedFormats;
    await this._getVideoElement();
    return;
  }

  /**
   *  开始扫描
   * @param targetedFormats 扫描文本类型
   * @param cameraDirection 摄像头位置
   */
  async startScan(
    targetedFormats: SupportedFormat = SupportedFormat.QR_CODE,
    cameraDirection: CameraDirection = CameraDirection.BACK
  ) {
    this._direction = cameraDirection;
    this._formats = targetedFormats;
    const video = await this._getVideoElement();
    if (video) {
      return await this._getFirstResultFromReader();
    } else {
      throw Error("Missing video element");
    }
  }

  /**
   * 暂停扫描
   */
  async pauseScanning() {
    if (!this._promiseOutR.is_finished) {
      this._promiseOutR.resolve(new Response());
    }
    await this.nativeFetch(`/stop`);
  }

  /**
   * 恢复扫描
   */
  async resumeScanning(): Promise<void> {
    await this._getFirstResultFromReader();
  }

  /**
   *  停止扫描
   * @param forceStopScan 是否强制停止扫描
   */
  async stopScan(_forceStopScan?: boolean): Promise<void> {
    this._stop();
    await this.nativeFetch(`/stop`);
  }

  /**
   *  检查是否有摄像头权限，如果没有或者被拒绝，那么会强制请求打开权限(设置)
   * @param forceCheck 是否强制检查权限
   */
  async checkCameraPermission(
    _forceCheck?: boolean,
    _beforeOpenPermissionSettings?: () => boolean | Promise<boolean>
  ) {
    if (typeof navigator === "undefined" || !navigator.permissions) {
      throw Error("Permissions API not available in this browser");
    }
    try {
      // https://developer.mozilla.org/en-US/docs/Web/API/Permissions/query
      // 所支持的特定权限因实现该功能的浏览器而异
      // 权限 API，所以我们需要一个 try/catch 以防 'camera' 无效
      const permission = await window.navigator.permissions.query({
        // deno-lint-ignore no-explicit-any
        name: "camera" as any,
      });
      if (permission.state === "prompt") {
        return {
          neverAsked: true,
        };
      }
      if (permission.state === "denied") {
        return {
          denied: true,
        };
      }
      if (permission.state === "granted") {
        return {
          granted: true,
        };
      }
      return {
        unknown: true,
      };
    } catch {
      throw Error("Camera permissions are not available in this browser");
    }
  }

  /**
   * 打开/关闭手电筒
   */
  // async toggleTorch() {
  //   return await this.nativeFetch("/toggleTorch")
  // };

  /**
   * 手电筒状态
   */
  // async getTorchState() {
  //   return await this.nativeFetch("/torchState")
  // };

  /**
   * 隐藏webview背景
   */
  // deno-lint-ignore require-await
  async hideBackground(): Promise<void> {
    this._backgroundColor = document.documentElement.style.backgroundColor;
    document.documentElement.style.backgroundColor = "transparent";
    return;
  }

  /**
   * 显示webview背景
   */
  // deno-lint-ignore require-await
  async showBackground(): Promise<void> {
    document.documentElement.style.backgroundColor =
      this._backgroundColor || "";
    return;
  }

  /**
   * 启动摄像
   * @returns
   */
  // deno-lint-ignore ban-types
  private _startVideo(): Promise<{}> {
    // deno-lint-ignore no-async-promise-executor
    return new Promise(async (resolve, reject) => {
      await navigator.mediaDevices
        .getUserMedia({
          audio: false,
          video: { facingMode: this._direction },
        })
        .then((stream: MediaStream) => {
          // 停止任何现有流，以便我们可以根据用户输入请求具有不同约束的媒体
          stream.getTracks().forEach((track) => track.stop());
        })
        .catch((error) => {
          reject(error);
        });

      const body = document.body;
      const video = document.getElementById("video");

      if (!video) {
        const parent = document.createElement("div");
        parent.setAttribute(
          "style",
          "position:absolute; top: 0; left: 0; width:100%; height: 100%; background-color: black;"
        );
        this._video = document.createElement("video");
        this._video.id = "video";
        // Don't flip video feed if camera is rear facing
        if (this._options?.cameraDirection !== CameraDirection.BACK) {
          this._video.setAttribute(
            "style",
            "-webkit-transform: scaleX(-1); transform: scaleX(-1); width:100%; height: 100%;"
          );
        } else {
          this._video.setAttribute("style", "width:100%; height: 100%;");
        }

        const userAgent = navigator.userAgent.toLowerCase();
        const isSafari =
          userAgent.includes("safari") && !userAgent.includes("chrome");

        // iOS 上的 Safari 需要设置 autoplay、muted 和 playsinline 属性，video.play() 才能成功
        // 如果没有这些属性，this.video.play() 将抛出 NotAllowedError
        // https://developer.apple.com/documentation/webkit/delivering_video_content_for_safari
        if (isSafari) {
          this._video.setAttribute("autoplay", "true");
          this._video.setAttribute("muted", "true");
          this._video.setAttribute("playsinline", "true");
        }

        parent.appendChild(this._video);
        body.appendChild(parent);

        if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
          const constraints: MediaStreamConstraints = {
            video: { facingMode: this._direction },
          };

          navigator.mediaDevices.getUserMedia(constraints).then(
            async (stream) => {
              //video.src = window.URL.createObjectURL(stream);
              if (this._video) {
                this._video.srcObject = stream;
                const videoTracks = stream.getAudioTracks()[0];
                const captureDevice = new ImageCapture(videoTracks);
                if (captureDevice) {
                  await captureDevice
                    .takePhoto()
                    .then(this.processPhoto)
                    .catch(this.stopCamera);
                }
                this._video.play();
              }
              resolve({});
            },
            (err) => {
              reject(err);
            }
          );
        }
      } else {
        reject({ message: "camera already started" });
      }
    });
  }

  private async _getVideoElement() {
    if (!this._video) {
      await this._startVideo();
    }
    return this._video;
  }
  /**
   * 返回扫码完的结果
   * @returns
   */
  private async _getFirstResultFromReader() {
    this._promiseOutR = new PromiseOut();
    const videoElement = await this._getVideoElement();
    if (videoElement) {
      await this._promiseOutR.promise;
    }
  }
  private async processPhoto(blob: Blob) {
    await this.nativeFetch(`/process?rotation=${0}&formats=${this._formats}`, {
      method: "POST",
      body: blob,
    })
      .then((res) => {
        this._promiseOutR.resolve(res);
      })
      .catch((err) => {
        this._promiseOutR.reject(err);
      });
  }

  // deno-lint-ignore no-explicit-any
  private stopCamera(error: any) {
    console.error(error);
    this._stop(); // turn off the camera
  }

  // deno-lint-ignore no-explicit-any
  private async _stop(): Promise<any> {
    if (this._video) {
      this._video.pause();

      // deno-lint-ignore no-explicit-any
      const st: any = this._video.srcObject;
      const tracks = st.getTracks();

      for (let i = 0; i < tracks.length; i++) {
        const track = tracks[i];
        track.stop();
      }
      await this._video.parentElement?.remove();
      this._video = null;
    }
  }
}

export const barcodeScannerPlugin = new BarcodeScannerPlugin();
