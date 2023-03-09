// build/plugin/esm/_dnt.polyfills.js
if (!String.prototype.replaceAll) {
  String.prototype.replaceAll = function(str, newStr) {
    if (Object.prototype.toString.call(str).toLowerCase() === "[object regexp]") {
      return this.replace(str, newStr);
    }
    return this.replace(new RegExp(str, "g"), newStr);
  };
}

// build/plugin/esm/_dnt.shims.js
var dntGlobals = {};
var dntGlobalThis = createMergeProxy(globalThis, dntGlobals);
function createMergeProxy(baseObj, extObj) {
  return new Proxy(baseObj, {
    get(_target, prop, _receiver) {
      if (prop in extObj) {
        return extObj[prop];
      } else {
        return baseObj[prop];
      }
    },
    set(_target, prop, value) {
      if (prop in extObj) {
        delete extObj[prop];
      }
      baseObj[prop] = value;
      return true;
    },
    deleteProperty(_target, prop) {
      let success = false;
      if (prop in extObj) {
        delete extObj[prop];
        success = true;
      }
      if (prop in baseObj) {
        delete baseObj[prop];
        success = true;
      }
      return success;
    },
    ownKeys(_target) {
      const baseKeys = Reflect.ownKeys(baseObj);
      const extKeys = Reflect.ownKeys(extObj);
      const extKeysSet = new Set(extKeys);
      return [...baseKeys.filter((k) => !extKeysSet.has(k)), ...extKeys];
    },
    defineProperty(_target, prop, desc) {
      if (prop in extObj) {
        delete extObj[prop];
      }
      Reflect.defineProperty(baseObj, prop, desc);
      return true;
    },
    getOwnPropertyDescriptor(_target, prop) {
      if (prop in extObj) {
        return Reflect.getOwnPropertyDescriptor(extObj, prop);
      } else {
        return Reflect.getOwnPropertyDescriptor(baseObj, prop);
      }
    },
    has(_target, prop) {
      return prop in extObj || prop in baseObj;
    }
  });
}

// build/plugin/esm/src/components/registerPlugin.js
var Plugins = class {
  constructor() {
    Object.defineProperty(this, "map", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: /* @__PURE__ */ new Map()
    });
    Object.defineProperty(this, "registerWebPlugin", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: (plugin) => {
        this.map.set(plugin.proxy, plugin);
      }
    });
  }
};
var plugins = new Plugins();
dntGlobalThis.Capacitor ? "" : dntGlobalThis.Capacitor = { Plugins: {} };
dntGlobalThis.Capacitor.Plugins = new Proxy({}, {
  get(_target, proxy, receiver) {
    return plugins.map.get(proxy);
  }
});
var registerWebPlugin = plugins.registerWebPlugin;

// build/plugin/node_modules/image-capture/src/imagecapture.js
var ImageCapture = window.ImageCapture;
if (typeof ImageCapture === "undefined") {
  ImageCapture = class {
    /**
     * TODO https://www.w3.org/TR/image-capture/#constructors
     *
     * @param {MediaStreamTrack} videoStreamTrack - A MediaStreamTrack of the 'video' kind
     */
    constructor(videoStreamTrack) {
      if (videoStreamTrack.kind !== "video")
        throw new DOMException("NotSupportedError");
      this._videoStreamTrack = videoStreamTrack;
      if (!("readyState" in this._videoStreamTrack)) {
        this._videoStreamTrack.readyState = "live";
      }
      this._previewStream = new MediaStream([videoStreamTrack]);
      this.videoElement = document.createElement("video");
      this.videoElementPlaying = new Promise((resolve) => {
        this.videoElement.addEventListener("playing", resolve);
      });
      if (HTMLMediaElement) {
        this.videoElement.srcObject = this._previewStream;
      } else {
        this.videoElement.src = URL.createObjectURL(this._previewStream);
      }
      this.videoElement.muted = true;
      this.videoElement.setAttribute("playsinline", "");
      this.videoElement.play();
      this.canvasElement = document.createElement("canvas");
      this.canvas2dContext = this.canvasElement.getContext("2d");
    }
    /**
     * https://w3c.github.io/mediacapture-image/index.html#dom-imagecapture-videostreamtrack
     * @return {MediaStreamTrack} The MediaStreamTrack passed into the constructor
     */
    get videoStreamTrack() {
      return this._videoStreamTrack;
    }
    /**
     * Implements https://www.w3.org/TR/image-capture/#dom-imagecapture-getphotocapabilities
     * @return {Promise<PhotoCapabilities>} Fulfilled promise with
     * [PhotoCapabilities](https://www.w3.org/TR/image-capture/#idl-def-photocapabilities)
     * object on success, rejected promise on failure
     */
    getPhotoCapabilities() {
      return new Promise(function executorGPC(resolve, reject) {
        const MediaSettingsRange = {
          current: 0,
          min: 0,
          max: 0
        };
        resolve({
          exposureCompensation: MediaSettingsRange,
          exposureMode: "none",
          fillLightMode: "none",
          focusMode: "none",
          imageHeight: MediaSettingsRange,
          imageWidth: MediaSettingsRange,
          iso: MediaSettingsRange,
          redEyeReduction: false,
          whiteBalanceMode: "none",
          zoom: MediaSettingsRange
        });
        reject(new DOMException("OperationError"));
      });
    }
    /**
     * Implements https://www.w3.org/TR/image-capture/#dom-imagecapture-setoptions
     * @param {Object} photoSettings - Photo settings dictionary, https://www.w3.org/TR/image-capture/#idl-def-photosettings
     * @return {Promise<void>} Fulfilled promise on success, rejected promise on failure
     */
    setOptions(photoSettings = {}) {
      return new Promise(function executorSO(resolve, reject) {
      });
    }
    /**
     * TODO
     * Implements https://www.w3.org/TR/image-capture/#dom-imagecapture-takephoto
     * @return {Promise<Blob>} Fulfilled promise with [Blob](https://www.w3.org/TR/FileAPI/#blob)
     * argument on success; rejected promise on failure
     */
    takePhoto() {
      const self = this;
      return new Promise(function executorTP(resolve, reject) {
        if (self._videoStreamTrack.readyState !== "live") {
          return reject(new DOMException("InvalidStateError"));
        }
        self.videoElementPlaying.then(() => {
          try {
            self.canvasElement.width = self.videoElement.videoWidth;
            self.canvasElement.height = self.videoElement.videoHeight;
            self.canvas2dContext.drawImage(self.videoElement, 0, 0);
            self.canvasElement.toBlob(resolve);
          } catch (error) {
            reject(new DOMException("UnknownError"));
          }
        });
      });
    }
    /**
     * Implements https://www.w3.org/TR/image-capture/#dom-imagecapture-grabframe
     * @return {Promise<ImageBitmap>} Fulfilled promise with
     * [ImageBitmap](https://www.w3.org/TR/html51/webappapis.html#webappapis-images)
     * argument on success; rejected promise on failure
     */
    grabFrame() {
      const self = this;
      return new Promise(function executorGF(resolve, reject) {
        if (self._videoStreamTrack.readyState !== "live") {
          return reject(new DOMException("InvalidStateError"));
        }
        self.videoElementPlaying.then(() => {
          try {
            self.canvasElement.width = self.videoElement.videoWidth;
            self.canvasElement.height = self.videoElement.videoHeight;
            self.canvas2dContext.drawImage(self.videoElement, 0, 0);
            resolve(window.createImageBitmap(self.canvasElement));
          } catch (error) {
            reject(new DOMException("UnknownError"));
          }
        });
      });
    }
  };
}
window.ImageCapture = ImageCapture;

// build/plugin/esm/deps/deno.land/x/bnqkl_util@1.1.2/packages/extends-promise-is/index.js
var isPromiseLike = (value) => {
  return value instanceof Object && typeof value.then === "function";
};

// build/plugin/esm/deps/deno.land/x/bnqkl_util@1.1.2/packages/extends-promise-out/PromiseOut.js
var PromiseOut = class {
  constructor() {
    Object.defineProperty(this, "promise", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "is_resolved", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: false
    });
    Object.defineProperty(this, "is_rejected", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: false
    });
    Object.defineProperty(this, "is_finished", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: false
    });
    Object.defineProperty(this, "value", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "reason", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "resolve", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "reject", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_innerFinally", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_innerFinallyArg", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_innerThen", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_innerCatch", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    this.promise = new Promise((resolve, reject) => {
      this.resolve = (value) => {
        try {
          if (isPromiseLike(value)) {
            value.then(this.resolve, this.reject);
          } else {
            this.is_resolved = true;
            this.is_finished = true;
            resolve(this.value = value);
            this._runThen();
            this._innerFinallyArg = Object.freeze({
              status: "resolved",
              result: this.value
            });
            this._runFinally();
          }
        } catch (err) {
          this.reject(err);
        }
      };
      this.reject = (reason) => {
        this.is_rejected = true;
        this.is_finished = true;
        reject(this.reason = reason);
        this._runCatch();
        this._innerFinallyArg = Object.freeze({
          status: "rejected",
          reason: this.reason
        });
        this._runFinally();
      };
    });
  }
  onSuccess(innerThen) {
    if (this.is_resolved) {
      this.__callInnerThen(innerThen);
    } else {
      (this._innerThen || (this._innerThen = [])).push(innerThen);
    }
  }
  onError(innerCatch) {
    if (this.is_rejected) {
      this.__callInnerCatch(innerCatch);
    } else {
      (this._innerCatch || (this._innerCatch = [])).push(innerCatch);
    }
  }
  onFinished(innerFinally) {
    if (this.is_finished) {
      this.__callInnerFinally(innerFinally);
    } else {
      (this._innerFinally || (this._innerFinally = [])).push(innerFinally);
    }
  }
  _runFinally() {
    if (this._innerFinally) {
      for (const innerFinally of this._innerFinally) {
        this.__callInnerFinally(innerFinally);
      }
      this._innerFinally = void 0;
    }
  }
  __callInnerFinally(innerFinally) {
    queueMicrotask(async () => {
      try {
        await innerFinally(this._innerFinallyArg);
      } catch (err) {
        console.error("Unhandled promise rejection when running onFinished", innerFinally, err);
      }
    });
  }
  _runThen() {
    if (this._innerThen) {
      for (const innerThen of this._innerThen) {
        this.__callInnerThen(innerThen);
      }
      this._innerThen = void 0;
    }
  }
  _runCatch() {
    if (this._innerCatch) {
      for (const innerCatch of this._innerCatch) {
        this.__callInnerCatch(innerCatch);
      }
      this._innerCatch = void 0;
    }
  }
  __callInnerThen(innerThen) {
    queueMicrotask(async () => {
      try {
        await innerThen(this.value);
      } catch (err) {
        console.error("Unhandled promise rejection when running onSuccess", innerThen, err);
      }
    });
  }
  __callInnerCatch(innerCatch) {
    queueMicrotask(async () => {
      try {
        await innerCatch(this.value);
      } catch (err) {
        console.error("Unhandled promise rejection when running onError", innerCatch, err);
      }
    });
  }
};

// build/plugin/esm/src/helper/binary.js
var encodeUri = (url) => {
  return url.replaceAll("#", "%23");
};

// build/plugin/esm/src/helper/createSignal.js
var createSignal = () => {
  return new Signal();
};
var Signal = class {
  constructor() {
    Object.defineProperty(this, "_cbs", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: /* @__PURE__ */ new Set()
    });
    Object.defineProperty(this, "listen", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: (cb) => {
        this._cbs.add(cb);
        return () => this._cbs.delete(cb);
      }
    });
    Object.defineProperty(this, "emit", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: (...args) => {
        for (const cb of this._cbs) {
          cb.apply(null, args);
        }
      }
    });
    Object.defineProperty(this, "clear", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: () => {
        this._cbs.clear();
      }
    });
  }
};

// build/plugin/esm/src/components/basePlugin.js
var BasePlugin = class extends HTMLElement {
  // mmid:为对应组件的名称，proxy:为劫持对象的属性
  constructor(mmid, proxy) {
    super();
    Object.defineProperty(this, "mmid", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: mmid
    });
    Object.defineProperty(this, "proxy", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: proxy
    });
    Object.defineProperty(this, "createSignal", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: createSignal
    });
  }
  nativeFetch(url, init) {
    if (url instanceof Request) {
      return fetch(url, init);
    }
    const host = globalThis.location.host.replace("www", "api");
    const api = `https://${host}/${this.mmid}${encodeUri(url)}`;
    console.log("nativeFetch=>", api);
    return fetch(api, init);
  }
};

// build/plugin/esm/src/components/barcode-scanner/barcodeScanner.type.js
var SupportedFormat;
(function(SupportedFormat2) {
  SupportedFormat2["UPC_A"] = "UPC_A";
  SupportedFormat2["UPC_E"] = "UPC_E";
  SupportedFormat2["UPC_EAN_EXTENSION"] = "UPC_EAN_EXTENSION";
  SupportedFormat2["EAN_8"] = "EAN_8";
  SupportedFormat2["EAN_13"] = "EAN_13";
  SupportedFormat2["CODE_39"] = "CODE_39";
  SupportedFormat2["CODE_39_MOD_43"] = "CODE_39_MOD_43";
  SupportedFormat2["CODE_93"] = "CODE_93";
  SupportedFormat2["CODE_128"] = "CODE_128";
  SupportedFormat2["CODABAR"] = "CODABAR";
  SupportedFormat2["ITF"] = "ITF";
  SupportedFormat2["ITF_14"] = "ITF_14";
  SupportedFormat2["AZTEC"] = "AZTEC";
  SupportedFormat2["DATA_MATRIX"] = "DATA_MATRIX";
  SupportedFormat2["MAXICODE"] = "MAXICODE";
  SupportedFormat2["PDF_417"] = "PDF_417";
  SupportedFormat2["QR_CODE"] = "QR_CODE";
  SupportedFormat2["RSS_14"] = "RSS_14";
  SupportedFormat2["RSS_EXPANDED"] = "RSS_EXPANDED";
})(SupportedFormat || (SupportedFormat = {}));
var CameraDirection;
(function(CameraDirection2) {
  CameraDirection2["FRONT"] = "user";
  CameraDirection2["BACK"] = "environment";
})(CameraDirection || (CameraDirection = {}));

// build/plugin/esm/src/components/barcode-scanner/barcodeScanner.plugin.js
var BarcodeScanner = class extends BasePlugin {
  constructor(mmid = "scanning.sys.dweb") {
    super(mmid, "BarcodeScanner");
    Object.defineProperty(this, "mmid", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: mmid
    });
    Object.defineProperty(this, "_formats", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: SupportedFormat.QR_CODE
    });
    Object.defineProperty(this, "_direction", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: CameraDirection.BACK
    });
    Object.defineProperty(this, "_video", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: null
    });
    Object.defineProperty(this, "_options", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: null
    });
    Object.defineProperty(this, "_backgroundColor", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: null
    });
    Object.defineProperty(this, "_promiseOutR", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: new PromiseOut()
    });
  }
  /**
   *  准备扫描
   * @param targetedFormats 扫描文本类型
   * @param cameraDirection 摄像头位置
   */
  async prepare(targetedFormats = SupportedFormat.QR_CODE, cameraDirection = CameraDirection.BACK) {
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
  async startScan(targetedFormats = SupportedFormat.QR_CODE, cameraDirection = CameraDirection.BACK) {
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
  async resumeScanning() {
    await this._getFirstResultFromReader();
  }
  /**
   *  停止扫描
   * @param forceStopScan 是否强制停止扫描
   */
  async stopScan(_forceStopScan) {
    this._stop();
    await this.nativeFetch(`/stop`);
  }
  /**
   *  检查是否有摄像头权限，如果没有或者被拒绝，那么会强制请求打开权限(设置)
   * @param forceCheck 是否强制检查权限
   */
  async checkCameraPermission(_forceCheck, _beforeOpenPermissionSettings) {
    if (typeof navigator === "undefined" || !navigator.permissions) {
      throw Error("Permissions API not available in this browser");
    }
    try {
      const permission = await globalThis.navigator.permissions.query({
        // deno-lint-ignore no-explicit-any
        name: "camera"
      });
      if (permission.state === "prompt") {
        return {
          neverAsked: true
        };
      }
      if (permission.state === "denied") {
        return {
          denied: true
        };
      }
      if (permission.state === "granted") {
        return {
          granted: true
        };
      }
      return {
        unknown: true
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
  async hideBackground() {
    this._backgroundColor = document.documentElement.style.backgroundColor;
    document.documentElement.style.backgroundColor = "transparent";
    return;
  }
  /**
   * 显示webview背景
   */
  // deno-lint-ignore require-await
  async showBackground() {
    document.documentElement.style.backgroundColor = this._backgroundColor || "";
    return;
  }
  /**
   * 启动摄像
   * @returns
   */
  // deno-lint-ignore ban-types
  _startVideo() {
    return new Promise(async (resolve, reject) => {
      await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: { facingMode: this._direction }
      }).then((stream) => {
        stream.getTracks().forEach((track) => track.stop());
      }).catch((error) => {
        reject(error);
      });
      const body = document.body;
      const video = document.getElementById("video");
      if (!video) {
        const parent = document.createElement("div");
        parent.setAttribute("style", "position:absolute; top: 0; left: 0; width:100%; height: 100%; background-color: black;");
        this._video = document.createElement("video");
        this._video.id = "video";
        if (this._options?.cameraDirection !== CameraDirection.BACK) {
          this._video.setAttribute("style", "-webkit-transform: scaleX(-1); transform: scaleX(-1); width:100%; height: 100%;");
        } else {
          this._video.setAttribute("style", "width:100%; height: 100%;");
        }
        const userAgent = navigator.userAgent.toLowerCase();
        const isSafari = userAgent.includes("safari") && !userAgent.includes("chrome");
        if (isSafari) {
          this._video.setAttribute("autoplay", "true");
          this._video.setAttribute("muted", "true");
          this._video.setAttribute("playsinline", "true");
        }
        parent.appendChild(this._video);
        body.appendChild(parent);
        if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
          const constraints = {
            video: { facingMode: this._direction }
          };
          navigator.mediaDevices.getUserMedia(constraints).then(async (stream) => {
            if (this._video) {
              this._video.srcObject = stream;
              const videoTracks = stream.getAudioTracks()[0];
              const captureDevice = new ImageCapture(videoTracks);
              if (captureDevice) {
                await captureDevice.takePhoto().then(this.processPhoto).catch(this.stopCamera);
              }
              this._video.play();
            }
            resolve({});
          }, (err) => {
            reject(err);
          });
        }
      } else {
        reject({ message: "camera already started" });
      }
    });
  }
  async _getVideoElement() {
    if (!this._video) {
      await this._startVideo();
    }
    return this._video;
  }
  /**
   * 返回扫码完的结果
   * @returns
   */
  async _getFirstResultFromReader() {
    this._promiseOutR = new PromiseOut();
    const videoElement = await this._getVideoElement();
    if (videoElement) {
      await this._promiseOutR.promise;
    }
  }
  async processPhoto(blob) {
    await this.nativeFetch(`/process?rotation=${0}&formats=${this._formats}`, {
      method: "POST",
      body: blob
    }).then((res) => {
      this._promiseOutR.resolve(res);
    }).catch((err) => {
      this._promiseOutR.reject(err);
    });
  }
  // deno-lint-ignore no-explicit-any
  stopCamera(error) {
    console.error(error);
    this._stop();
  }
  // deno-lint-ignore no-explicit-any
  async _stop() {
    if (this._video) {
      this._video.pause();
      const st = this._video.srcObject;
      const tracks = st.getTracks();
      for (let i = 0; i < tracks.length; i++) {
        const track = tracks[i];
        track.stop();
      }
      await this._video.parentElement?.remove();
      this._video = null;
    }
  }
};

// build/plugin/esm/src/components/barcode-scanner/index.js
customElements.define("dweb-scanner", BarcodeScanner);
document.addEventListener("DOMContentLoaded", documentOnDOMContentLoaded);
function documentOnDOMContentLoaded() {
  const el = new BarcodeScanner();
  document.body.append(el);
  document.removeEventListener("DOMContentLoaded", documentOnDOMContentLoaded);
}

// build/plugin/esm/src/components/navigator-bar/navigator.events.js
var NavigationBarPluginEvents;
(function(NavigationBarPluginEvents2) {
  NavigationBarPluginEvents2["SHOW"] = "onShow";
  NavigationBarPluginEvents2["HIDE"] = "onHide";
  NavigationBarPluginEvents2["COLOR_CHANGE"] = "onColorChange";
})(NavigationBarPluginEvents || (NavigationBarPluginEvents = {}));

// build/plugin/esm/src/components/navigator-bar/navigator-bar.js
var Navigatorbar = class extends BasePlugin {
  constructor(mmid = "navigationBar.sys.dweb") {
    super(mmid, "NavigationBar");
    Object.defineProperty(this, "mmid", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: mmid
    });
    Object.defineProperty(this, "_signalShow", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: this.createSignal()
    });
    Object.defineProperty(this, "_signalHide", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: this.createSignal()
    });
    Object.defineProperty(this, "_signalChange", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: this.createSignal()
    });
  }
  connectedCallback() {
  }
  /**
  * 显示导航栏。
  */
  async show() {
  }
  /**
   * 隐藏导航栏。
   */
  async hide() {
  }
  /**
   * 更改导航栏的颜色。
   *支持 alpha 十六进制数。
   * @param options
   */
  async setColor(options) {
  }
  /**
   * 设置透明度
   * @param isTransparent
   */
  async setTransparency(options) {
  }
  /**
   * 以十六进制获取导航栏的当前颜色。
   */
  async getColor() {
    return { color: " " };
  }
  /**
   * 导航栏显示后触发的事件
   * @param event The event
   * @param listenerFunc Callback
   * NavigationBarPluginEvents.HIDE 导航栏隐藏后触发的事件
   * NavigationBarPluginEvents.COLOR_CHANGE 导航栏颜色更改后触发的事件
   */
  addListener(event, listenerFunc) {
    switch (event) {
      case NavigationBarPluginEvents.HIDE:
        return this._signalHide.listen(listenerFunc);
      case NavigationBarPluginEvents.COLOR_CHANGE:
        return this._signalChange.listen(listenerFunc);
      default:
        return this._signalShow.listen(listenerFunc);
    }
  }
};

// build/plugin/esm/src/components/navigator-bar/navigator.type.js
var NAVIGATION_BAR_COLOR;
(function(NAVIGATION_BAR_COLOR2) {
  NAVIGATION_BAR_COLOR2["TRANSPARENT"] = "#00000000";
  NAVIGATION_BAR_COLOR2["WHITE"] = "#ffffff";
  NAVIGATION_BAR_COLOR2["BLACK"] = "#000000";
})(NAVIGATION_BAR_COLOR || (NAVIGATION_BAR_COLOR = {}));

// build/plugin/esm/src/components/navigator-bar/index.js
customElements.define("dweb-navigator", Navigatorbar);
document.addEventListener("DOMContentLoaded", documentOnDOMContentLoaded2);
function documentOnDOMContentLoaded2() {
  const el = new Navigatorbar();
  document.body.append(el);
  document.removeEventListener("DOMContentLoaded", documentOnDOMContentLoaded2);
}

// build/plugin/esm/src/helper/color.js
function convertToRGBAHex(color) {
  let colorHex = "#";
  if (color.startsWith("rgba(")) {
    const colorArr = color.replace("rgba(", "").replace(")", "").split(",");
    for (let [index, item] of colorArr.entries()) {
      if (index === 3) {
        item = `${parseFloat(item) * 255}`;
      }
      let itemHex = Math.round(parseFloat(item)).toString(16);
      if (itemHex.length === 1) {
        itemHex = "0" + itemHex;
      }
      colorHex += itemHex;
    }
  }
  if (color.startsWith("#")) {
    if (color.length === 9) {
      colorHex = color;
    } else {
      color = color.substring(1);
      if (color.length === 4 || color.length === 3) {
        color = color.replace(/(.)/g, "$1$1");
      }
      colorHex += color.padEnd(8, "F");
    }
  }
  return colorHex;
}

// build/plugin/esm/src/components/statusbar/statusbar.type.js
var StatusbarStyle;
(function(StatusbarStyle2) {
  StatusbarStyle2["Dark"] = "DARK";
  StatusbarStyle2["Light"] = "LIGHT";
  StatusbarStyle2["Default"] = "DEFAULT";
})(StatusbarStyle || (StatusbarStyle = {}));
var EStatusBarAnimation;
(function(EStatusBarAnimation2) {
  EStatusBarAnimation2["None"] = "NONE";
  EStatusBarAnimation2["Slide"] = "SLIDE";
  EStatusBarAnimation2["Fade"] = "FADE";
})(EStatusBarAnimation || (EStatusBarAnimation = {}));

// build/plugin/esm/src/components/statusbar/statusbar.plugin.js
var StatusbarPlugin = class extends BasePlugin {
  // mmid 最好全部采用小写，防止出现不可预期的意外
  constructor(mmid = "statusbar.sys.dweb") {
    super(mmid, "StatusBar");
    Object.defineProperty(this, "mmid", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: mmid
    });
    Object.defineProperty(this, "_visible", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: true
    });
    Object.defineProperty(this, "_style", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: StatusbarStyle.Default
    });
    Object.defineProperty(this, "_color", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: ""
    });
    Object.defineProperty(this, "_overlays", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: false
    });
  }
  /**
   * 设置状态栏背景色
   * @param r 0~255
   * @param g 0~255
   * @param b 0~255
   * @param a 0~1
   */
  async setBackgroundColor(options) {
    const colorHex = convertToRGBAHex(options.color ?? "");
    return await this.nativeFetch(`/setBackgroundColor?color=${colorHex}`);
  }
  /**
   *  获取背景颜色
   */
  async getBackgroundColor() {
    return await this.nativeFetch(`/getBackgroundColor`);
  }
  /**
   * 设置状态栏风格
   * // 支持 light | dark | defalt
   * 据观测
   * 在系统主题为 Light 的时候, Default 意味着 白色字体
   * 在系统主题为 Dark 的手, Default 因为这 黑色字体
   * 这兴许与设置有关系, 无论如何, 尽可能避免使用 Default 带来的不确定性
   *
   * @param style
   */
  async setStyle(styleOptions) {
    await this.nativeFetch(`/setStyle?style=${styleOptions.style}`);
  }
  /**
   * 获取当前style
   * @returns
   */
  async getStyle() {
    return (await this.getInfo()).style;
  }
  /**
  * 显示状态栏。
  * 在 iOS 上，如果状态栏最初是隐藏的，并且初始样式设置为
  * `UIStatusBarStyleLightContent`，第一次显示调用可能会在
  * 动画将文本显示为深色然后过渡为浅色。 值得推荐
  * 在第一次调用时使用 `Animation.None` 作为动画。
  *
  * @since 1.0.0
  */
  async show(options) {
    const animation = options?.animation ?? EStatusBarAnimation.None;
    await this.nativeFetch(`/setVisible?visible=true&animation=${animation}`);
  }
  /**
   * Hide the status bar.
   *
   * @since 1.0.0
   */
  async hide(options) {
    const animation = options?.animation ?? EStatusBarAnimation.None;
    await this.nativeFetch(`/setVisible?visible=false&animation=${animation}`);
  }
  /**
  * 获取有关状态栏当前状态的信息。
  *
  * @since 1.0.0
  */
  async getInfo() {
    const result = await this.nativeFetch(`/getInfo`).then((res) => res.json()).catch((err) => err);
    return result;
  }
  /**
  * 设置状态栏是否应该覆盖 webview 以允许使用
  * 它下面的空间。
  *
  * 此方法仅在 Android 上支持。
  *
  * @since 1.0.0
  */
  async setOverlaysWebView(options) {
    await this.nativeFetch(`/setOverlays?overlay=${options.overlay}`);
  }
};

// build/plugin/esm/src/components/statusbar/index.js
customElements.define("dweb-statusbar", StatusbarPlugin);
document.addEventListener("DOMContentLoaded", documentOnDOMContentLoaded3);
function documentOnDOMContentLoaded3() {
  const el = new StatusbarPlugin();
  document.body.append(el);
  document.removeEventListener("DOMContentLoaded", documentOnDOMContentLoaded3);
}

// build/plugin/esm/src/components/toast/toast.plugin.js
var ToastPlugin = class extends BasePlugin {
  constructor(mmid = "toast.sys.dweb") {
    super(mmid, "Toast");
    Object.defineProperty(this, "mmid", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: mmid
    });
    Object.defineProperty(this, "_root", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: void 0
    });
    Object.defineProperty(this, "_elStyle", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: document.createElement("style")
    });
    Object.defineProperty(this, "_fragment", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: new DocumentFragment()
    });
    Object.defineProperty(this, "_duration", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: "long"
    });
    Object.defineProperty(this, "_position", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: "bottom"
    });
    Object.defineProperty(this, "_verticalClassName", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: ""
    });
    this._root = this.attachShadow({ mode: "open" });
    this._init();
  }
  _init() {
  }
  // private _initfragment() {
  //     this._fragment.append(this._elContent, this._elStyle);
  //     return this;
  // }
  // private _initShadowRoot() {
  //     this._root.appendChild(this._fragment)
  //     return this;
  // }
  // private _initContent() {
  //     this._elContent.setAttribute("class", "content")
  //     this._elContent.innerText = '消息的内容！';
  //     return this;
  // }
  // private _initStyle() {
  //     this._elStyle.setAttribute("type", "text/css")
  //     this._elStyle.innerText = createCssText()
  //     return this;
  // }
  /**
   * toast信息显示
   * @param message 消息
   * @param duration 时长 'long' | 'short'
   * @returns
   */
  async show(options) {
    const { text, duration = "long", position = "bottom" } = options;
    this._duration = duration;
    this._position = position;
    this.setAttribute("style", "left: 0px;");
    return await this.nativeFetch(`/show?message=${text}&duration=${duration}&position=${position}`);
  }
  connectedCallback() {
  }
};

// build/plugin/esm/src/components/toast/index.js
customElements.define("dweb-toast", ToastPlugin);
document.addEventListener("DOMContentLoaded", documentOnDOMContentLoaded4);
function documentOnDOMContentLoaded4() {
  const el = new ToastPlugin();
  document.body.append(el);
  document.removeEventListener("DOMContentLoaded", documentOnDOMContentLoaded4);
}

// build/plugin/esm/src/components/torch/torch.plugin.js
var TorchPlugin = class extends BasePlugin {
  constructor(mmid = "torch.sys.dweb") {
    super(mmid, "Torch");
    Object.defineProperty(this, "mmid", {
      enumerable: true,
      configurable: true,
      writable: true,
      value: mmid
    });
  }
  /**
   * 打开/关闭手电筒
   */
  async toggleTorch() {
    return await this.nativeFetch("/toggleTorch");
  }
  /**
   * 手电筒状态
   */
  async getTorchState() {
    return await this.nativeFetch("/torchState");
  }
};

// build/plugin/esm/src/components/torch/index.js
customElements.define("dweb-torch", TorchPlugin);
document.addEventListener("DOMContentLoaded", documentOnDOMContentLoaded5);
function documentOnDOMContentLoaded5() {
  const el = new TorchPlugin();
  document.body.append(el);
  document.removeEventListener("DOMContentLoaded", documentOnDOMContentLoaded5);
}

// build/plugin/esm/src/components/index.js
registerWebPlugin(new Navigatorbar());
registerWebPlugin(new BarcodeScanner());
registerWebPlugin(new StatusbarPlugin());
registerWebPlugin(new ToastPlugin());
registerWebPlugin(new TorchPlugin());

// demo/src/index.ts
function $(params) {
  return document.getElementById(params);
}
document.addEventListener("DOMContentLoaded", () => {
  $("toast-show").addEventListener("click", async () => {
    const toast = document.querySelector("dweb-toast");
    console.log("click toast-show");
    const duration = $("toast-duration").value ?? "long";
    const text = $("toast-message").value ?? "\u6211\u662Ftoast\u{1F353}";
    if (toast) {
      const result = await toast.show({ text, duration }).then((res) => res.text());
      $("statusbar-observer-log").innerHTML = result;
    }
  });
  const statusBar = document.querySelector("dweb-statusbar");
  $("statusbar-setBackgroundColor").addEventListener("click", async () => {
    const color = $("statusbar-background-color").value;
    console.log("statusbar=>", color);
    const result = await statusBar.setBackgroundColor({ color }).then((res) => res.text());
    console.log("statusbar-setBackgroundColor=>", result);
    $("statusbar-observer-log").innerHTML = result;
  });
  $("statusbar-getBackgroundColor").addEventListener("click", async () => {
    const result = await statusBar.getBackgroundColor().then((res) => res.text());
    console.log("statusbar-getBackgroundColor=>", result);
    $("statusbar-observer-log").innerHTML = result;
  });
  $("statusbar-setStyle").addEventListener("click", async () => {
    const styleOptions = $("statusbar-style").value;
    await statusBar.setStyle({ style: styleOptions });
  });
  $("statusbar-getStyle").addEventListener("click", async () => {
    const result = await statusBar.getStyle();
    console.log("statusbar-getBackgroundColor=>", result);
    $("statusbar-observer-log").innerHTML = result;
  });
  $("statusbar-show").addEventListener("click", async () => {
    const animation = $("statusbar-animation").value;
    await statusBar.show({ animation });
  });
  $("statusbar-hide").addEventListener("click", async () => {
    const animation = $("statusbar-animation").value;
    await statusBar.hide({ animation });
  });
  $("statusbar-setOverlaysWebView").addEventListener("click", async () => {
    const overlay = $("statusbar-overlay").checked;
    await statusBar.setOverlaysWebView({ overlay });
  });
  $("statusbar-getOverlaysWebView").addEventListener("click", async () => {
    const result = await statusBar.getInfo();
    $("statusbar-observer-log").innerHTML = JSON.stringify(result);
  });
});
/**
 * String.prototype.replaceAll() polyfill
 * https://gomakethings.com/how-to-replace-a-section-of-a-string-with-another-one-with-vanilla-js/
 * @author Chris Ferdinandi
 * @license MIT
 */
/*! Bundled license information:

image-capture/src/imagecapture.js:
  (**
   * MediaStream ImageCapture polyfill
   *
   * @license
   * Copyright 2018 Google Inc.
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   *      http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   *)
*/
