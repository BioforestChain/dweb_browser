import { PromiseOut } from "../../helper/PromiseOut.ts";
import { cacheGetter } from "../../helper/cacheGetter.ts";
import { CloseWatcher } from "../close-watcher/index.ts";
import { barcodeScannerPlugin } from "./barcode-scanning.plugin.ts";
import {
  BarcodeScannerPermission,
  CameraDirection,
  ScanOptions,
  ScanResult,
  ScannerProcesser,
} from "./barcode-scanning.type.ts";

const html = String.raw;
// const asStyle = (cssText: string) => {
//   const styleText = cssText
//     .match(/\{([\w\W]*)\}/)![1]
//     .trim()
//     .replace(/\n/g, " ");
//   return styleText;
// };
export class HTMLDwebBarcodeScanningElement extends HTMLElement {
  static readonly tagName = "dweb-barcode-scanning";
  readonly plugin = barcodeScannerPlugin;

  private readonly _dialog: HTMLDialogElement | null = null;
  private readonly _video: HTMLVideoElement;
  private readonly _canvas: HTMLCanvasElement;
  private readonly _ctx: CanvasRenderingContext2D;
  private controller: ScannerProcesser | undefined;
  // private _formats: SupportedFormat | undefined;
  private _activity?: PromiseOut<string[]>;
  private _rotation?: number = 0;
  private _isCloceLock = false;

  constructor() {
    super();
    const shadow = this.attachShadow({ mode: "open" });
    let mainHtml = html`<canvas slot="canvas"></canvas>
      <video muted autoplay hidden playsinline></video>
      <style>
        :host {
          display: flex;
          justify-content: center;
          align-items: center;
        }
        video {
          display: none;
        }
        video::-webkit-media-controls {
          display: none !important;
        }

        video::-webkit-media-controls-start-playback-button {
          display: none !important;
          -webkit-appearance: none;
        }

        canvas {
          object-fit: cover;
          max-width: 100%;
          max-height: 100%;
        }
        dialog {
          max-height: unset;
          max-width: unset;
          padding: 0;
          border: 0;
          margin: 0;
          width: 100%;
          height: 100%;

          user-select: none;
          -webkit-user-select: none;
        }
        dialog::backdrop {
          background-color: rgba(0, 0, 0, 0.5);
          pointer-events: none;
          -webkit-backdrop-filter: blur(20px);
          backdrop-filter: blur(20px);
        }
      </style>`;
    if (HTMLDwebBarcodeScanningElement._support_native_request_full_screen === false) {
      mainHtml = html`<dialog>${mainHtml}</dialog>`;
    }
    shadow.innerHTML = mainHtml;
    this._dialog = shadow.querySelector("dialog");
    this._dialog?.addEventListener("close", () => {
      this.stopScanning();
    });
    this._dialog?.addEventListener("dblclick", () => {
      this._dialog?.close();
    });
    this._canvas = shadow.querySelector("canvas")!;
    this._video = shadow.querySelector("video")!;
    this._ctx = this._canvas.getContext("2d")!;
  }
  static readonly _support_native_request_full_screen = false; // typeof document.body.requestFullscreen === "function";

  override async requestFullscreen(options?: FullscreenOptions) {
    const dialog = this._dialog;
    if (dialog === null) {
      return super.requestFullscreen(options);
    }
    dialog.showModal();

    if (this._isCloceLock == false) {
      const closer = new CloseWatcher();
      dialog.onclose = () => closer.close();

      this._isCloceLock = true;
      closer.addEventListener("close", (_event) => {
        this._isCloceLock = false;
        dialog.onclose = null;
        dialog.close();
      });
    }
  }

  async connectedCallback() {
    if (!this.controller) {
      this.controller = await this.plugin.createProcesser();
    }
  }

  disconnectedCallback() {
    this.stopScanning();
    this.controller?.stop();
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
   * 启动扫码，webComponent版
   * @param scanOptions
   * @returns ScanResult
   * @since 1.0.0
   */
  async startScanning(scanOptions?: ScanOptions): Promise<ScanResult> {
    const rotation = scanOptions?.rotation;
    const direction = scanOptions?.direction ?? CameraDirection.BACK;
    // const formats = scanOptions?.formats ?? SupportedFormat.QR_CODE;
    // 有更改的时候才发送更新
    if (rotation && this._rotation != rotation) {
      this._rotation = rotation;
      this.controller?.setRotation(this._rotation);
    }
    try {
      const permission = await this._startVideo(direction, scanOptions?.width, scanOptions?.height);
      let data: string[] = [];
      if (permission === BarcodeScannerPermission.UserAgree) {
        this.requestFullscreen({ navigationUI: "show" });
        data = await this.taskPhoto();
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
   * @since 1.0.0
   */
  stopScanning() {
    if (this._activity !== undefined) {
      this._activity.resolve([]);
      this._activity = undefined;
    }
    if (this._video.srcObject) {
      /// 插件停止处理
      this.plugin.stop();

      /// 视频停止播放
      this._video.pause();
      // const st = this._video.srcObject;
      // if (st instanceof MediaStream) {
      //   const tracks = st.getTracks();
      //   for (let i = 0; i < tracks.length; i++) {
      //     const track = tracks[i];
      //     track.stop();
      //   }
      // }
      this._video.srcObject = null;
    }
    if (this.rafId) {
      cancelAnimationFrame(this.rafId);
      this.rafId = undefined;
    }

    this._dialog?.close();
  }

  private taskPhoto() {
    if (this._activity === undefined) {
      this._taskPhoto((this._activity = new PromiseOut()));
    } else {
      console.warn("already running taskPhoto");
    }
    return this._activity.promise;
  }

  /**
   * 不断识图的任务
   * @returns
   */
  private async _taskPhoto(task: PromiseOut<string[]>) {
    if (this._canvas === null) {
      console.error("service close！");
      return [];
    }
    try {
      const result = await (async () => {
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
        const waitFrame = () => new Promise((resolve) => requestAnimationFrame(resolve));
        while (task.readyState === PromiseOut.PENDING) {
          await waitFrame();
          const result = await this.controller?.process(await toBlob());
          if (result && result.length != 0) {
            return result.map((it) => it.data);
          }
        }
        return [];
      })();
      task.resolve(result);
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
  private async _startVideo(direction: CameraDirection, width?: number, height?: number) {
    // 判断是否支持
    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
      const constraints: MediaStreamConstraints = {
        video: {
          facingMode: direction,
          width: { exact: width ?? innerWidth * devicePixelRatio },
          height: { exact: height ?? innerHeight * devicePixelRatio },
        },
      };
      console.log("video window=>", width, height);
      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      try {
        await this.gotMedia(stream);
        return BarcodeScannerPermission.UserAgree;
      } catch (e) {
        console.warn("You need to authorize the camera permission to use the scan code!", e);
        this.stopScanning();
        return BarcodeScannerPermission.UserReject;
      }
    } else {
      this.stopScanning();
      console.error("Your browser does not support scanning code!");
      return BarcodeScannerPermission.UserError;
    }
  }

  private rafId?: number;
  /**
   * 拿到帧对象
   * @param mediastream
   */
  private async gotMedia(mediastream: MediaStream) {
    this._video.srcObject = mediastream;
    await new Promise((resolve, reject) => {
      this._video.onloadedmetadata = resolve;
      this._video.onerror = reject;
    });
    this._canvas.width = this._video.videoWidth;
    this._canvas.height = this._video.videoHeight;
    this._canvas.style.width = this._canvas.width + "px";
    this._canvas.style.height = this._canvas.height + "px";
    const videoTracks = mediastream.getVideoTracks();
    if (videoTracks.length > 0 && this._canvas) {
      this._canvas.captureStream(100);
      // 压缩为 100 * 100
      const update = () => {
        this.rafId = requestAnimationFrame(() => {
          this._ctx.drawImage(this._video, 0, 0, this._canvas.width, this._canvas.height);
          update();
        });
      };
      update();
    }
    // this.requestFullscreen?.({ navigationUI: "show" });
  }

  // document.exitFullscreen?.();
}
if (!customElements.get(HTMLDwebBarcodeScanningElement.tagName)) {
  customElements.define(HTMLDwebBarcodeScanningElement.tagName, HTMLDwebBarcodeScanningElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebBarcodeScanningElement.tagName]: HTMLDwebBarcodeScanningElement;
  }
}
