import { ImageCapture } from "../../../deps.ts";
import { BasePlugin } from "../basePlugin.ts";
import { CameraDirection } from "./camera.type.ts";

export class CameraPlugin extends BasePlugin {
  tagName = "camera.sys.dweb"

  private _video: HTMLVideoElement | null = null;

  /**
   * 启动摄像
   * @returns
   */
  // deno-lint-ignore ban-types
  // private _startVideo(direction: CameraDirection): Promise<{}> {
  //   // deno-lint-ignore no-async-promise-executor
  //   return new Promise(async (resolve, reject) => {
  //     await navigator.mediaDevices
  //       .getUserMedia({
  //         audio: false,
  //         video: { facingMode: direction },
  //       })
  //       .then((stream: MediaStream) => {
  //         // 停止任何现有流，以便我们可以根据用户输入请求具有不同约束的媒体
  //         stream.getTracks().forEach((track) => track.stop());
  //       })
  //       .catch((error) => {
  //         reject(error);
  //       });

  //     const body = document.body;
  //     const video = document.getElementById("video");

  //     if (!video) {
  //       const parent = document.createElement("div");
  //       parent.setAttribute(
  //         "style",
  //         "position:absolute; top: 0; left: 0; width:100%; height: 100%; background-color: black;"
  //       );
  //       this._video = document.createElement("video");
  //       this._video.id = "video";
  //       // Don't flip video feed if camera is rear facing
  //       if (direction !== CameraDirection.BACK) {
  //         this._video.setAttribute(
  //           "style",
  //           "-webkit-transform: scaleX(-1); transform: scaleX(-1); width:100%; height: 100%;"
  //         );
  //       } else {
  //         this._video.setAttribute("style", "width:100%; height: 100%;");
  //       }

  //       const userAgent = navigator.userAgent.toLowerCase();
  //       const isSafari =
  //         userAgent.includes("safari") && !userAgent.includes("chrome");

  //       // iOS 上的 Safari 需要设置 autoplay、muted 和 playsinline 属性，video.play() 才能成功
  //       // 如果没有这些属性，this.video.play() 将抛出 NotAllowedError
  //       // https://developer.apple.com/documentation/webkit/delivering_video_content_for_safari
  //       if (isSafari) {
  //         this._video.setAttribute("autoplay", "true");
  //         this._video.setAttribute("muted", "true");
  //         this._video.setAttribute("playsinline", "true");
  //       }

  //       parent.appendChild(this._video);
  //       body.appendChild(parent);

  //       if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
  //         const constraints: MediaStreamConstraints = {
  //           video: { facingMode: direction },
  //         };

  //         navigator.mediaDevices.getUserMedia(constraints).then(
  //           async (stream) => {
  //             //video.src = window.URL.createObjectURL(stream);
  //             if (this._video) {
  //               this._video.srcObject = stream;
  //               const videoTracks = stream.getAudioTracks()[0];
  //               const captureDevice = new ImageCapture(videoTracks);
  //               if (captureDevice) {
  //                 await captureDevice
  //                   .takePhoto()
  //                   .then(this.processPhoto)
  //                   .catch(this.stopCamera);
  //               }
  //               this._video.play();
  //             }
  //             resolve({});
  //           },
  //           (err) => {
  //             reject(err);
  //           }
  //         );
  //       }
  //     } else {
  //       reject({ message: "camera already started" });
  //     }
  //   });
  // }

  // getPhoto(options: ImageOptions): Promise<Photo> {

  // }
}
