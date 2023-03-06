import { BasePlugin } from '../../basePlugin';
import { CameraDirection, ScanOptions, ScanResult, SupportedFormat } from './barcodeScanner.type.cjs';

export class BarcodeScanner extends BasePlugin {

  private _direction: string = "environment";
  private _video: HTMLVideoElement | null = null;
  private _options: ScanOptions | null = null;
  private _backgroundColor: string | null = null;

  constructor(readonly mmid = "file://scanning.sys.dweb") {
    super(mmid);
  }

  /**
   *  准备扫描
   * @param targetedFormats 扫描文本类型
   * @param cameraDirection 摄像头位置
   */
  async prepare(): Promise<void> {
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
    this._direction = cameraDirection
    const video = await this._getVideoElement();
    if (video) {
      return await this._getFirstResultFromReader(targetedFormats);
    } else {
      throw Error('Missing video element');
    }
  }

  /**
   *  检查是否有摄像头权限，如果没有或者被拒绝，那么会强制请求打开权限(设置)
   * @param forceCheck 是否强制检查权限
   */
  checkPermission() {
    if (typeof navigator === 'undefined' || !navigator.permissions) {
      throw Error('Permissions API not available in this browser');
    }
  }

  async hideBackground(): Promise<void> {
    this._backgroundColor = document.documentElement.style.backgroundColor;
    document.documentElement.style.backgroundColor = 'transparent';
    return;
  }

  async showBackground(): Promise<void> {
    document.documentElement.style.backgroundColor = this._backgroundColor || '';
    return;
  }

  private async _getFirstResultFromReader(targetedFormats: SupportedFormat) {
    const videoElement = await this._getVideoElement();
    return new Promise(async (resolve) => {
      if (videoElement) {
        const stream = await this.getVideoSteam(videoElement);
        this.nativeFetch(`process?rotation=${0}&formats=${targetedFormats}`, {
          method: "POST",
          body: stream
        }).then(res => {
          resolve(res)
        })
      }
    });
  }

  /**
   * 启动摄像
   * @returns 
   */
  private async _startVideo(): Promise<{}> {
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
      const video = document.getElementById('video');

      if (!video) {
        const parent = document.createElement('div');
        parent.setAttribute(
          'style',
          'position:absolute; top: 0; left: 0; width:100%; height: 100%; background-color: black;'
        );
        this._video = document.createElement('video');
        this._video.id = 'video';
        // Don't flip video feed if camera is rear facing
        if (this._options?.cameraDirection !== CameraDirection.BACK) {
          this._video.setAttribute(
            'style',
            '-webkit-transform: scaleX(-1); transform: scaleX(-1); width:100%; height: 100%;'
          );
        } else {
          this._video.setAttribute('style', 'width:100%; height: 100%;');
        }

        const userAgent = navigator.userAgent.toLowerCase();
        const isSafari = userAgent.includes('safari') && !userAgent.includes('chrome');

        // iOS 上的 Safari 需要设置 autoplay、muted 和 playsinline 属性，video.play() 才能成功
        // 如果没有这些属性，this.video.play() 将抛出 NotAllowedError
        // https://developer.apple.com/documentation/webkit/delivering_video_content_for_safari
        if (isSafari) {
          this._video.setAttribute('autoplay', 'true');
          this._video.setAttribute('muted', 'true');
          this._video.setAttribute('playsinline', 'true');
        }

        parent.appendChild(this._video);
        body.appendChild(parent);

        if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
          const constraints: MediaStreamConstraints = {
            video: { facingMode: this._direction },
          };

          navigator.mediaDevices.getUserMedia(constraints).then(
            (stream) => {
              //video.src = window.URL.createObjectURL(stream);
              if (this._video) {
                this._video.srcObject = stream;
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
        reject({ message: 'camera already started' });
      }
    });
  }

  private async _getVideoElement() {
    if (!this._video) {
      await this._startVideo();
    }
    return this._video;
  }

  private async _stop(): Promise<any> {
    if (this._video) {
      this._video.pause();

      const st: any = this._video.srcObject;
      const tracks = st.getTracks();

      for (var i = 0; i < tracks.length; i++) {
        var track = tracks[i];
        track.stop();
      }
      this._video.parentElement?.remove();
      this._video = null;
    }
  }
  /**
   * 绘制并转换为一帧图片
   * @param video 
   * @returns 
   */
  private getVideoSteam(video: HTMLVideoElement): Promise<ReadableStream> {
    return new Promise(function (resolve, reject) {
      video.setAttribute('crossOrigin', 'anonymous');//处理跨域
      video.addEventListener('loadeddata', function () {
        let canvas = document.createElement("canvas")
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        canvas.getContext('2d')?.drawImage(video, 0, 0, canvas.width, canvas.height);
        // 每帧捕获的次数
        const stream = canvas.captureStream(25);
        const read = new ReadableStream({
          start(controller) {
            stream.addEventListener("close", () => {
              controller.close()
            })
            controller.enqueue(stream)
          },
        })
        resolve(read);
      });
    })
  }
}
