// src/sys/plugins/components/basePlugin.ts
var BasePlugin = class extends HTMLElement {
};

// src/sys/plugins/components/statusbar/statusbar.plugin.mts
var StatusbarPlugin = class extends BasePlugin {
  constructor() {
    super();
    this._statusbarHttpAddress = "./operation_from_plugins";
    this._appUrl = void 0;
    this._appUrl = location.origin;
  }
  /**
   * 设置状态栏的颜色
   * @param color 
   * @returns 
   */
  async setBackgroundColor(color) {
    return this._set("set_background_color", color);
  }
  // 支持 light | dark | defalt
  async setStyle(style) {
    if (style !== "light" && style !== "dark" && style !== "default")
      return console.error("\u8BBE\u7F6E\u72B6\u6001\u680Fstyle\u51FA\u9519\uFF0C\u975E\u6CD5\u7684\u53C2\u6570\uFF01");
    return this._set("set_style", style);
  }
  // 获取状态栏样式
  async getStyle() {
    return this._set("get_style", "");
  }
  // 获取statusbar的高度
  async getHeight() {
    return this._set("get_height", "");
  }
  /**
   * 设置状态栏是否覆盖
   * @param value 
   */
  setOverlaysWebview(value) {
    if (value !== "0" && value !== "1")
      throw new Error('setOverlaysWebview \u975E\u6CD5\u7684\u53C2\u6570 \u53C2\u6570\u8303\u56F4 "0" | "1" \u5F53\u524D\u53C2\u6570===' + value);
    return this._set("set_overlays", value);
  }
  getOverlaysWebview() {
    return this._set("get_overlays", "");
  }
  async _set(action, value) {
    if (this._statusbarHttpAddress === void 0)
      return console.error("this._statusbarHttpAddress === undefined");
    const result = await fetch(
      `${this._statusbarHttpAddress}?app_url=${this._appUrl}`,
      {
        method: "PUT",
        body: JSON.stringify({ action, value }),
        headers: {
          "Content-Type": "application/json; charset=UTF-8",
          "Plugin-Target": "statusbar"
        }
      }
    );
    return Promise.resolve(JSON.parse(await result.json()).value);
  }
};
customElements.define("statusbar-dweb", StatusbarPlugin);
document.addEventListener("DOMContentLoaded", documentOnDOMContentLoaded);
function documentOnDOMContentLoaded() {
  const el = new StatusbarPlugin();
  document.body.append(el);
  document.removeEventListener("DOMContentLoaded", documentOnDOMContentLoaded);
}

// src/sys/plugins/components/toast/toast.plugin.mts
var ToastPlugin = class extends HTMLElement {
  constructor() {
    super();
    this._elContent = document.createElement("div");
    this._elStyle = document.createElement("style");
    this._fragment = new DocumentFragment();
    this._duration = "long";
    this._position = "bottom";
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
  show(message, duration = "long", position = "bottom") {
    this._duration = duration;
    this._position = position;
    this.setAttribute("style", "left: 0px;");
    this._elContent.innerText = message;
    this._elContentTransitionStart();
    return this;
  }
  connectedCallback() {
  }
};
customElements.define("toast-dweb", ToastPlugin);
document.addEventListener("DOMContentLoaded", documentOnDOMContentLoaded2);
function documentOnDOMContentLoaded2() {
  const el = new ToastPlugin();
  document.body.append(el);
  document.removeEventListener("DOMContentLoaded", documentOnDOMContentLoaded2);
}
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
