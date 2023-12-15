import { PromiseOut } from "../../helper/PromiseOut.ts";
import { cacheGetter } from "../../helper/cacheGetter.ts";
import { CameraDirection } from "../camera/camera.type.ts";
import { CloseWatcher } from "../close-watcher/index.ts";
import { barcodeScannerPlugin } from "./barcode-scanning.plugin.ts";
import { BarcodeScannerPermission, ScanResult, SupportedFormat } from "./barcode-scanning.type.ts";

export class HTMLDwebBarcodeScanningElement extends HTMLElement {
  static readonly tagName = "dweb-barcode-scanning";
  readonly plugin = barcodeScannerPlugin;

  private _video: HTMLVideoElement | null = null;
  private _canvas: HTMLCanvasElement | null = null;
  private _formats = SupportedFormat.QR_CODE;
  private _direction: string = CameraDirection.BACK;
  private _activity?: PromiseOut<string[]>;
  private _isCloceLock = false;

  constructor() {
    super();
  }

  private createClose() {
    const closer = new CloseWatcher();
    this._isCloceLock = true;
    closer.addEventListener("close", (_event) => {
      this._isCloceLock = false;
      if (this._activity !== undefined) {
        this.stopScanning();
      }
    });
  }

  /**
   * 返回扫码页面DOM
   * 根据这个DOM 用户可以自定义样式
   * @returns HTMLElement
   */
  @cacheGetter()
  get getView() {
    if (this._video) {
      return this._video.parentElement;
    }
    return null;
  }

  @cacheGetter()
  get process() {
    return this.plugin.createProcesser;
  }
  @cacheGetter()
  get stop() {
    return this.plugin.stop;
  }

  /**
   * 启动扫码
   * @returns
   */
  async startScanning(rotation = 0, formats = SupportedFormat.QR_CODE): Promise<ScanResult> {
    try {
      if (!this._isCloceLock) {
        this.createClose();
      }
      await this.createElement();
      const permission = await this._startVideo();
      let data: string[] = [];
      if (permission === BarcodeScannerPermission.UserAgree) {
        data = await this.taskPhoto(rotation, formats);
      }
      return {
        hasContent: data.length !== 0,
        content: data,
        permission,
      };
    } finally {
      this.stopScanning();
    }
  }
  /**
   * 停止扫码
   */
  stopScanning() {
    if (this._activity !== undefined) {
      this._activity.resolve([]);
      this._activity = undefined;
    }
    this.stopCamera("user stop");
  }

  // deno-lint-ignore no-explicit-any
  private stopCamera(error?: any) {
    if (error) {
      console.error(error);
    }
    this._stop();
  }
  async taskPhoto(rotation: number, formats: SupportedFormat) {
    if (this._activity === undefined) {
      this._taskPhoto((this._activity = new PromiseOut()), rotation, formats);
    } else {
      console.warn("already running taskPhoto");
    }
    return this._activity.promise;
  }

  /**
   * 不断识图的任务
   * @returns
   */
  private async _taskPhoto(task: PromiseOut<string[]>, rotation: number, formats: SupportedFormat) {
    if (this._canvas === null) {
      console.error("service close！");
      return [];
    }
    try {
      task.resolve(
        (async () => {
          const controller = await this.plugin.createProcesser(formats);
          controller.setRotation(rotation);
          const toBlob = (quality = 0.8) => {
            const blob = new PromiseOut<Blob>();
            const canvas = this._canvas;
            if (canvas) {
              canvas.toBlob(
                (imageBlob) => {
                  if (imageBlob) {
                    blob.resolve(imageBlob);
                  } else {
                    blob.reject("canvas fail to toBlob");
                  }
                },
                "image/jpeg",
                quality
              );
            } else {
              blob.reject("canvas stop");
            }
            return blob.promise;
          };
          const waitFrame = () => {
            const frame = new PromiseOut<void>();
            requestAnimationFrame(() => frame.resolve());
            return frame.promise;
          };
          while (task.readyState === PromiseOut.PENDING) {
            await waitFrame();
            const result = await controller.process(await toBlob());
            if (result.length != 0) {
              return result.map((it) => it.data);
            }
          }
          return [];
        })()
      );
    } catch (e) {
      task.reject(e);
    } finally {
      task.resolve([]);
    }
  }

  /**
   * 启动摄像
   * @returns
   */
  private async _startVideo() {
    // 判断是否支持
    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
      const constraints: MediaStreamConstraints = {
        video: { facingMode: this._direction },
      };
      return await navigator.mediaDevices
        .getUserMedia(constraints)
        .then(async (stream) => {
          await this.gotMedia(stream);
          return BarcodeScannerPermission.UserAgree;
        })
        .catch((e) => {
          console.error("You need to authorize the camera permission to use the scan code!", e);
          this.stopScanning();
          return BarcodeScannerPermission.UserReject;
        });
    } else {
      this.stopScanning();
      console.error("Your browser does not support scanning code!");
      return BarcodeScannerPermission.UserError;
    }
  }

  /**
   * 拿到帧对象
   * @param mediastream
   */
  private async gotMedia(mediastream: MediaStream) {
    if (!this._video) {
      throw new Error("not create video");
    }
    this._video.srcObject = mediastream;
    const videoTracks = mediastream.getVideoTracks();
    if (videoTracks.length > 0 && this._canvas) {
      this._canvas.captureStream(100);
      const ctx = this._canvas.getContext("2d");
      // 压缩为 100 * 100
      const update = () =>
        requestAnimationFrame(() => {
          if (ctx && this._video) {
            ctx.drawImage(this._video, 0, 0, this._canvas?.width ?? 480, this._canvas?.height ?? 360);
            update();
          }
        });
      update();
    }
    await this._video.play();
    this._video?.parentElement?.setAttribute(
      "style",
      `
      position:fixed; top: 0; left: 0; width:100%; height: 100%; background-color: black;
      -webkit-transition:all 0.2s linear;
      -moz-transition:all 0.2s linear;
      -ms-transition:all 0.2s linear;
      -o-transition:all 0.2s linear;
      transition:all 0.2s linear;
      visibility: visible;`
    );
  }

  private _stop() {
    if (this._video) {
      this._video.pause();

      const st = this._video.srcObject;
      if (st) {
        // deno-lint-ignore no-explicit-any
        const tracks = (st as any).getTracks();

        for (let i = 0; i < tracks.length; i++) {
          const track = tracks[i];
          track.stop();
        }
      }

      this._video.parentElement?.remove();
      this._video = null;
    }
    if (this._canvas) {
      this._canvas.getContext("2d")?.clearRect(0, 0, this._canvas.width, this._canvas.height);
      this._canvas = null;
    }
  }

  /**
   * 创建video
   * @param direction
   * @returns
   */
  private createElement(direction: CameraDirection = CameraDirection.BACK) {
    return new Promise((resolve, reject) => {
      const body = document.body;

      const video = document.getElementById("video");
      const canvas = document.getElementById("canvas");

      if (video) {
        reject("camera already started");
        return { message: "camera already started" };
      }
      const parent = document.createElement("div");
      parent.setAttribute("class", "plaoc-scanning");
      // parent.setAttribute(
      //   "style",
      //   "position:fixed; top: 0; left: 0; width:100%; height: 100%; background-color: black;visibility: hidden;"
      // );
      this._video = document.createElement("video");
      this._video.id = "video";

      // 如果摄像头朝后，请勿翻转视频源
      if (direction !== CameraDirection.BACK) {
        this._video.setAttribute(
          "style",
          "-webkit-transform: scaleX(-1); transform: scaleX(-1); width:100%; height: 100%;"
        );
      } else {
        this._video.setAttribute("style", "width:100%; height: 100%;");
      }
      this._video.setAttribute("autoplay", "true");

      const userAgent = navigator.userAgent.toLowerCase();
      const isSafari = userAgent.includes("safari") && !userAgent.includes("chrome");

      // iOS 上的 Safari 需要设置 autoplay、muted 和 playsinline 属性，video.play() 才能成功
      // 如果没有这些属性，this.video.play() 将抛出 NotAllowedError
      // https://developer.apple.com/documentation/webkit/delivering_video_content_for_safari
      if (isSafari) {
        this._video.setAttribute("muted", "true");
        this._video.setAttribute("playsinline", "true");
      }

      parent.appendChild(this._video);
      if (!canvas) {
        this._canvas = document.createElement("canvas");
        this._canvas.setAttribute("style", "visibility: hidden;");
        this._canvas.width = 480;
        this._canvas.height = 360;
        this._canvas.id = "canvas";
        parent.appendChild(this._canvas);
      }
      body.appendChild(parent);
      resolve(true);
    });
  }
}
if (!customElements.get(HTMLDwebBarcodeScanningElement.tagName)) {
  customElements.define(HTMLDwebBarcodeScanningElement.tagName, HTMLDwebBarcodeScanningElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebBarcodeScanningElement.tagName]: HTMLDwebBarcodeScanningElement;
  }
}
