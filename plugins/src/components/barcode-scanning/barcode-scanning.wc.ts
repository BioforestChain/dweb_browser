/// <reference path="../../shims/ImageCapture.d.ts" />
import { cacheGetter } from "../../helper/cacheGetter.ts";
import { CameraDirection } from "../camera/camera.type.ts";
import { CloseWatcher } from "../close-watcher/close-watcher.shim.ts";
import { torchPlugin } from "../torch/torch.plugin.ts";
import { barcodeScannerPlugin } from "./barcode-scanning.plugin.ts";
import { SupportedFormat } from "./barcode-scanning.type.ts";

export class HTMLDwebBarcodeScanningElement extends HTMLElement {
  plugin = barcodeScannerPlugin;

  private _video: HTMLVideoElement | null = null;
  private _canvas: HTMLCanvasElement | null = null;
  private _formats = SupportedFormat.QR_CODE;
  private _direction: string = CameraDirection.BACK;
  private _activity = false

  constructor() {
    super();
    const closer = new CloseWatcher();
    closer.addEventListener("close", (event) => {
      console.log("CloseWatcher stopScanning", event.isTrusted, event.timeStamp);
      if (this._activity) {
        this.stopScanning()
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
    return this._video?.parentElement
  }

  @cacheGetter()
  get process() {
    return this.plugin.process
  }
  @cacheGetter()
  get stop() {
    return this.plugin.stop
  }

  @cacheGetter()
  get toggleTorch() {
    return torchPlugin.toggleTorch
  }

  @cacheGetter()
  get getTorchState() {
    return torchPlugin.getTorchState
  }

  // @cacheGetter()
  // get getPhoto() {
  //   return this.plugin.getPhoto
  // }

  /**
   * 看看是否支持扫码
   * @returns boolean
   */
  hasMedia = () => {
    return (navigator.getUserMedia =
      navigator.getUserMedia ||
      navigator.mozGetUserMedia ||
      navigator.webkitGetUserMedia);
  };
  /**
   * 启动扫码
   * @returns 
   */
  async startScanning(rotation = 0, formats = SupportedFormat.QR_CODE) {
    this.createElement();
    await this._startVideo();
    return await this.taskPhoto(rotation, formats);
  }
  /**
   * 停止扫码
   */
  stopScanning() {
    this._activity = false
    this.stopCamera("user stop")
  }


  // deno-lint-ignore no-explicit-any
  private stopCamera(error: any) {
    console.error(error);
    this._stop();
  }

  /**
   * 不断识图的任务
   * @returns 
   */
  private taskPhoto(rotation: number, formats: SupportedFormat): Promise<string[]> {
    this._activity = true
    return new Promise((resolve, reject) => {
      const task = () => {
        if (!this._canvas) return reject("service close！")
        if (!this._activity) return reject("user close")
        this._canvas.toBlob(
          async (imageBlob) => {
            if (imageBlob) {
              const value = await this.plugin
                .process(imageBlob, rotation, formats)
                .then((res) => res)
                .catch(() => {
                  this._activity = false
                  return reject("502 service error");
                });
              const result = Array.from(value ?? []);
              if (result.length > 0) {
                this.stopCamera(result);
                this._activity = false
                return resolve(result);
              }
              return task();
            }
          },
          "image/jpeg",
          0.5 // lossy compression
        );
      }
      task()
    })
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

      await navigator.mediaDevices
        .getUserMedia(constraints)
        .then((stream) => {
          this.gotMedia(stream);
        })
        .catch((e) => {
          console.error("getUserMedia() failed: ", e);
          throw new Error(
            "You need to authorize the camera permission to use the scan code!"
          );
        });
    } else {
      throw new Error("Your browser does not support scanning code!");
    }
  }

  /**
   * 拿到帧对象
   * @param mediastream
   */
  private gotMedia(mediastream: MediaStream) {
    if (!this._video) {
      throw new Error("not create video");
    }
    this._video.srcObject = mediastream;
    const videoTracks = mediastream.getVideoTracks();
    if (videoTracks.length > 0 && this._canvas) {
      this._canvas.captureStream(100)
      this._video.parentElement?.setAttribute("style", `
      position:fixed; top: 0; left: 0; width:100%; height: 100%; background-color: black;
      -webkit-transition:all 0.5s linear;
      -moz-transition:all 0.5s linear;
      -ms-transition:all 0.5s linear;
      -o-transition:all 0.5s linear;
      transition:all 0.5s linear;
      visibility: visible;`);
      const ctx = this._canvas.getContext("2d");
      // 压缩为 100 * 100
      const update = () => requestAnimationFrame(() => {
        if (ctx && this._video) {
          ctx.drawImage(this._video, 0, 0, this._canvas?.width ?? 480, this._canvas?.height ?? 360);
          update()
        }
      });
      update()
    }
    this._video.play();
  }

  private _stop() {
    if (this._video) {
      this._video.pause();

      // deno-lint-ignore no-explicit-any
      const st: any = this._video.srcObject;
      const tracks = st.getTracks();

      for (let i = 0; i < tracks.length; i++) {
        const track = tracks[i];
        track.stop();
      }
      this._video.parentElement?.remove();
      this._video = null;
    }
    if (this._canvas) {
      this._canvas.getContext("2d")?.clearRect(0, 0, this._canvas.width, this._canvas.height)
      this._canvas = null
    }
  }

  /**
   * 创建video
   * @param direction
   * @returns
   */
  private createElement(direction: CameraDirection = CameraDirection.BACK) {
    const body = document.body;

    const video = document.getElementById("video");
    const canvas = document.getElementById("canvas");

    if (video) {
      return { message: "camera already started" };
    }
    const parent = document.createElement("div");
    parent.setAttribute(
      "style",
      "position:fixed; top: 0; left: 0; width:100%; height: 100%; background-color: black;visibility: hidden;"
    );
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
    const isSafari =
      userAgent.includes("safari") && !userAgent.includes("chrome");

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
      this._canvas.width = 480
      this._canvas.height = 360
      this._canvas.id = "canvas";
      parent.appendChild(this._canvas);
    }
    body.appendChild(parent);
  }
}

customElements.define(
  barcodeScannerPlugin.tagName,
  HTMLDwebBarcodeScanningElement
);
