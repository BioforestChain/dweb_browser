/// <reference path="../../shims/ImageCapture.d.ts" />
import { CameraDirection } from "../camera/camera.type.ts";
import { barcodeScannerPlugin } from "./barcode-scanning.plugin.ts";
import { SupportedFormat } from "./barcode-scanning.type.ts";

export class HTMLDwebBarcodeScanningElement extends HTMLElement {
  plugin = barcodeScannerPlugin;

  private _video: HTMLVideoElement | null = null;
  private _canvas: HTMLCanvasElement | null = null;
  private _formats = SupportedFormat.QR_CODE;
  private _direction: string = CameraDirection.BACK;
  // private _imageCapturer: ImageCapture | null = null

  constructor() {
    super();
  }

  hasMedia = () => {
    return (navigator.getUserMedia =
      navigator.getUserMedia ||
      navigator.mozGetUserMedia ||
      navigator.webkitGetUserMedia);
  };

  async startScan() {
    this.createElement();
    await this._startVideo();
    return await this.taskPhoto()
  }

  // deno-lint-ignore no-explicit-any
  async taskPhoto(): Promise<any> {
    //   .then((res) => res)
    //   .catch(this.stopCamera);
    if (!this._canvas) return;

    this._canvas.toBlob(async (imageBlob) => {
      if (imageBlob) {
        console.log("当前截取的帧=>", imageBlob.size)
        const value = await this.plugin
          .process(imageBlob)
          .then((res) => res.json())
          .catch((e) => {
            console.log("识别失败:", e);
            return e
          });
        const result = Array.from(value)
        console.log("识别到扫码对象=>", result, result.length);
        if (result.length > 0) {
          this.stopCamera(result);
          return result;
        }
        return await this.taskPhoto()
      }

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
        .then(stream => {
          this.gotMedia(stream)
        })
        .catch((e) => {
          console.error("getUserMedia() failed: ", e);
        });
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
      this._canvas.captureStream(25)
      const ctx = this._canvas.getContext('2d')
      // 压缩为 100 * 100
      ctx?.drawImage(this._video, 100, 100)
      // this._imageCapturer = new ImageCapture(videoTracks[0]);
    }
    this._video.play();
  }

  /**
   * 停止扫码
   * @param error
   */
  // deno-lint-ignore no-explicit-any
  stopCamera(error: any) {
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

  /**
   * 创建video
   * @param direction
   * @returns
   */
  createElement(direction: CameraDirection = CameraDirection.BACK) {
    const body = document.body;

    const video = document.getElementById("video");
    const canvas = document.getElementById("canvas");

    if (video) {
      return { message: "camera already started" };
    }
    const parent = document.createElement("div");
    parent.setAttribute(
      "style",
      "position:absolute; top: 0; left: 0; width:100%; height: 100%; background-color: black;"
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
    body.appendChild(parent);

    if (!canvas) {
      this._canvas = document.createElement("canvas");
      this._canvas.id = "canvas";
      body.appendChild(this._canvas)
    }
  }
}

customElements.define(
  barcodeScannerPlugin.tagName,
  HTMLDwebBarcodeScanningElement
);
