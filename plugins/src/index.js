// src/components/registerPlugin.ts
var hiJackCapacitorPlugin = window.Capacitor.Plugins;
var registerWebPlugin = (plugin) => {
  new Proxy(hiJackCapacitorPlugin, {
    get(_target, key) {
      if (key === plugin.proxy) {
        return plugin;
      }
    }
  });
};

// deps.ts
import { ImageCapture } from "https://esm.sh/image-capture@0.4.0";
import { PromiseOut } from "https://deno.land/x/bnqkl_util@1.1.2/packages/extends-promise-out/index.ts";

// src/helper/createSignal.ts
var createSignal = () => {
  return new Signal();
};
var Signal = class {
  constructor() {
    this._cbs = /* @__PURE__ */ new Set();
    this.listen = (cb) => {
      this._cbs.add(cb);
      return () => this._cbs.delete(cb);
    };
    this.emit = (...args) => {
      for (const cb of this._cbs) {
        cb.apply(null, args);
      }
    };
    this.clear = () => {
      this._cbs.clear();
    };
  }
};

// src/components/basePlugin.ts
var BasePlugin = class extends HTMLElement {
  // mmid:为对应组件的名称，proxy:为劫持对象的属性
  constructor(mmid, proxy) {
    super();
    this.mmid = mmid;
    this.proxy = proxy;
    this.createSignal = createSignal;
  }
  nativeFetch(url, init) {
    if (url instanceof Request) {
      return fetch(url, init);
    }
    return fetch(new URL(url, this.mmid), init);
  }
};

// src/components/barcode-scanner/barcodeScanner.type.ts
var SupportedFormat = /* @__PURE__ */ ((SupportedFormat2) => {
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
  return SupportedFormat2;
})(SupportedFormat || {});
var CameraDirection = /* @__PURE__ */ ((CameraDirection2) => {
  CameraDirection2["FRONT"] = "user";
  CameraDirection2["BACK"] = "environment";
  return CameraDirection2;
})(CameraDirection || {});

// src/components/barcode-scanner/barcodeScanner.plugin.ts
var BarcodeScanner = class extends BasePlugin {
  constructor(mmid = "scanning.sys.dweb") {
    super(mmid, "BarcodeScanner");
    this.mmid = mmid;
    this._formats = "QR_CODE" /* QR_CODE */;
    this._direction = "environment" /* BACK */;
    this._video = null;
    this._options = null;
    this._backgroundColor = null;
    this._promiseOutR = new PromiseOut();
  }
  /**
   *  准备扫描
   * @param targetedFormats 扫描文本类型
   * @param cameraDirection 摄像头位置
   */
  async prepare(targetedFormats = "QR_CODE" /* QR_CODE */, cameraDirection = "environment" /* BACK */) {
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
  async startScan(targetedFormats = "QR_CODE" /* QR_CODE */, cameraDirection = "environment" /* BACK */) {
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
      const permission = await window.navigator.permissions.query({
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
        parent.setAttribute(
          "style",
          "position:absolute; top: 0; left: 0; width:100%; height: 100%; background-color: black;"
        );
        this._video = document.createElement("video");
        this._video.id = "video";
        if (this._options?.cameraDirection !== "environment" /* BACK */) {
          this._video.setAttribute(
            "style",
            "-webkit-transform: scaleX(-1); transform: scaleX(-1); width:100%; height: 100%;"
          );
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
          navigator.mediaDevices.getUserMedia(constraints).then(
            async (stream) => {
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

// src/components/barcode-scanner/index.ts
customElements.define("dweb-scanner", BarcodeScanner);
document.addEventListener("DOMContentLoaded", documentOnDOMContentLoaded);
function documentOnDOMContentLoaded() {
  const el = new BarcodeScanner();
  document.body.append(el);
  document.removeEventListener("DOMContentLoaded", documentOnDOMContentLoaded);
}

// src/components/navigator-bar/navigator.events.ts
var NavigationBarPluginEvents = /* @__PURE__ */ ((NavigationBarPluginEvents2) => {
  NavigationBarPluginEvents2["SHOW"] = "onShow";
  NavigationBarPluginEvents2["HIDE"] = "onHide";
  NavigationBarPluginEvents2["COLOR_CHANGE"] = "onColorChange";
  return NavigationBarPluginEvents2;
})(NavigationBarPluginEvents || {});

// src/components/navigator-bar/navigator-bar.ts
var Navigatorbar = class extends BasePlugin {
  constructor(mmid = "navigationBar.sys.dweb") {
    super(mmid, "NavigationBar");
    this.mmid = mmid;
    this._signalShow = this.createSignal();
    this._signalHide = this.createSignal();
    this._signalChange = this.createSignal();
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
      case "onHide" /* HIDE */:
        return this._signalHide.listen(listenerFunc);
      case "onColorChange" /* COLOR_CHANGE */:
        return this._signalChange.listen(listenerFunc);
      default:
        return this._signalShow.listen(listenerFunc);
    }
  }
};

// src/components/navigator-bar/navigator.type.ts
var NAVIGATION_BAR_COLOR = /* @__PURE__ */ ((NAVIGATION_BAR_COLOR2) => {
  NAVIGATION_BAR_COLOR2["TRANSPARENT"] = "#00000000";
  NAVIGATION_BAR_COLOR2["WHITE"] = "#ffffff";
  NAVIGATION_BAR_COLOR2["BLACK"] = "#000000";
  return NAVIGATION_BAR_COLOR2;
})(NAVIGATION_BAR_COLOR || {});

// src/components/navigator-bar/index.ts
customElements.define("dweb-navigator", Navigatorbar);
document.addEventListener("DOMContentLoaded", documentOnDOMContentLoaded2);
function documentOnDOMContentLoaded2() {
  const el = new Navigatorbar();
  document.body.append(el);
  document.removeEventListener("DOMContentLoaded", documentOnDOMContentLoaded2);
}

// src/components/statusbar/statusbar.plugin.ts
var StatusbarPlugin = class extends BasePlugin {
  constructor(mmid = "statusBar.sys.dweb") {
    super(mmid, "StatusBar");
    this.mmid = mmid;
  }
  /**
   * 设置状态栏背景色
   * @param r 0~255
   * @param g 0~255
   * @param b 0~255
   * @param a 0~1
   */
  async setBackgroundColor(options) {
  }
  // 支持 light | dark | defalt
  async setStyle(style) {
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
  }
  /**
   * Hide the status bar.
   *
   * @since 1.0.0
   */
  async hide(options) {
  }
  /**
  * 获取有关状态栏当前状态的信息。
  *
  * @since 1.0.0
  */
  // async getInfo(): Promise<StatusBarInfo> {
  //     return { visible :}
  // }
  /**
  * 设置状态栏是否应该覆盖 webview 以允许使用
  * 它下面的空间。
  *
  * 此方法仅在 Android 上支持。
  *
  * @since 1.0.0
  */
  async setOverlaysWebView(options) {
  }
};

// src/components/statusbar/index.ts
customElements.define("dweb-statusbar", StatusbarPlugin);
document.addEventListener("DOMContentLoaded", documentOnDOMContentLoaded3);
function documentOnDOMContentLoaded3() {
  const el = new StatusbarPlugin();
  document.body.append(el);
  document.removeEventListener("DOMContentLoaded", documentOnDOMContentLoaded3);
}

// src/components/toast/toast.plugin.ts
var ToastPlugin = class extends BasePlugin {
  constructor(mmid = "toast.sys.dweb") {
    super(mmid, "Toast");
    this.mmid = mmid;
    this._elContent = document.createElement("div");
    this._elStyle = document.createElement("style");
    this._fragment = new DocumentFragment();
    this._duration = "long";
    this._position = "bottom";
    // deno-lint-ignore no-inferrable-types
    this._verticalClassName = "";
    this._onTransitionenOutToIn = () => {
      setTimeout(() => {
        this._elContent.removeEventListener("transitionend", this._onTransitionenOutToIn);
        this._elContent.addEventListener("transitionend", this._onTransitionendInToOut);
        this._elContent.classList.remove("content_transform_inside");
        this._elContent.classList.add("content_transform_outside");
      }, this._duration === "long" ? 2e3 : 3500);
    };
    this._onTransitionendInToOut = () => {
      this._elContent.removeEventListener("transitionend", this._onTransitionendInToOut);
      this._elContent.classList.remove("content_transform_outside");
      this._elContent.classList.remove("content_transition");
      this._elContent.classList.remove(this._verticalClassName);
      this.setAttribute("style", "");
    };
    this._elContentTransitionStart = () => {
      this._verticalClassName = this._position === "bottom" ? "content_vertical_bottom" : this._position === "center" ? "content_vertical_center" : "content_vertical_top";
      this._elContent.classList.add("content_transform_outside", this._verticalClassName);
      this._elContent.addEventListener("transitionend", this._onTransitionenOutToIn);
      setTimeout(() => {
        this._elContent.classList.remove("content_transform_outside");
        this._elContent.classList.add("content_transform_inside", "content_transition");
      }, 100);
    };
    this._root = this.attachShadow({ mode: "open" });
    this._init();
  }
  _init() {
    this._initContent()._initStyle()._initfragment()._initShadowRoot();
  }
  _initfragment() {
    this._fragment.append(this._elContent, this._elStyle);
    return this;
  }
  _initShadowRoot() {
    this._root.appendChild(this._fragment);
    return this;
  }
  _initContent() {
    this._elContent.setAttribute("class", "content");
    this._elContent.innerText = "\u6D88\u606F\u7684\u5185\u5BB9\uFF01";
    return this;
  }
  _initStyle() {
    this._elStyle.setAttribute("type", "text/css");
    this._elStyle.innerText = createCssText();
    return this;
  }
  /**
   * toast信息显示
   * @param message 消息
   * @param duration 时长 'long' | 'short'
   * @returns
   */
  show(message, duration = "long", position = "bottom") {
    this._duration = duration;
    this._position = position;
    this.setAttribute("style", "left: 0px;");
    this._elContent.innerText = message;
    this._elContentTransitionStart();
    this.nativeFetch(`/show?message=${message}&duration=${duration}&position=${position}`);
    return this;
  }
  connectedCallback() {
  }
};
function createCssText() {
  return `
        :host{
            position: fixed;
            z-index: 9999999999;
            left: -100vw;
            top: 0;
            display: flex;
            justify-content: center;
            align-items: center;
            box-sizing: border-box;
            width: 100%;
            height: 100%;
            border: 1px solid red;
        }

        .content{
            position: absolute;
            box-sizing: border-box;
            padding: 0px 10px;
            min-width: 10px;
            max-width: 300;
            font-size: 13px;
            line-height: 30px;
            overflow: hidden;
            whilte-space: nowrap;
            text-overflow: ellipsis;
            border-radius: 10px;
            color: #fff;
            background: #000d;
        }
        
        .content_vertical_top{
            top: 10px;
        }

        .content_vertical_center{

        }

        .content_vertical_bottom{
            bottom: 30px;
        }

        .content_transform_outside{
            transform: translateX(100vw);
        }

        .content_transform_inside{
            transform: translateX(0);
        }

        .content_transition{
            transition: all 0.5s ease-out;
        }
    
    `;
}

// src/components/toast/index.ts
customElements.define("dweb-toast", ToastPlugin);
document.addEventListener("DOMContentLoaded", documentOnDOMContentLoaded4);
function documentOnDOMContentLoaded4() {
  const el = new ToastPlugin();
  document.body.append(el);
  document.removeEventListener("DOMContentLoaded", documentOnDOMContentLoaded4);
}

// src/components/torch/torch.plugin.ts
var TorchPlugin = class extends BasePlugin {
  constructor(mmid = "torch.sys.dweb") {
    super(mmid, "Torch");
    this.mmid = mmid;
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

// src/components/torch/index.ts
customElements.define("dweb-torch", TorchPlugin);
document.addEventListener("DOMContentLoaded", documentOnDOMContentLoaded5);
function documentOnDOMContentLoaded5() {
  const el = new TorchPlugin();
  document.body.append(el);
  document.removeEventListener("DOMContentLoaded", documentOnDOMContentLoaded5);
}

// src/components/index.ts
registerWebPlugin(new Navigatorbar());
registerWebPlugin(new BarcodeScanner());
registerWebPlugin(new StatusbarPlugin());
registerWebPlugin(new ToastPlugin());
registerWebPlugin(new TorchPlugin());
export {
  BarcodeScanner,
  CameraDirection,
  NAVIGATION_BAR_COLOR,
  NavigationBarPluginEvents,
  Navigatorbar,
  StatusbarPlugin,
  SupportedFormat,
  ToastPlugin,
  TorchPlugin
};
