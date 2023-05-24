// 效果 webview 容器
import { css, html, LitElement } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import { styleMap } from "lit/directives/style-map.js";
import { ifDefined } from "lit/directives/if-defined.js";
import { Webview } from "./multi-webview.mjs";
import ecxuteJavascriptCode from "./multi-webview-content-execute-javascript.mjs";
import WebviewTag = Electron.WebviewTag;

@customElement("multi-webview-content")
export class MultiWebViewContent extends LitElement {
  static override styles = createAllCSS();

  @property({ type: Webview }) customWebview: Webview | undefined = undefined;
  @property({ type: Boolean }) closing: Boolean = false;
  @property({ type: Number }) zIndex: Number = 0;
  @property({ type: Number }) scale: Number = 0;
  @property({ type: Number }) opacity: Number = 1;
  @property({ type: Number }) customWebviewId: Number = 0;
  @property({ type: String }) src: String = "";
  @property({ type: String }) preload: String = "";
  @state() statusbarHidden: boolean = false;
  @query("webview") elWebview: WebviewTag | undefined;

  onDomReady(event: Event) {
    this.dispatchEvent(
      new CustomEvent("dom-ready", {
        bubbles: true,
        detail: {
          customWebview: this.customWebview,
          event: event,
          from: event.target,
        },
      })
    );
    console.log("onDomReady");
  }

  webviewDidStartLoading(e: Event) {
    console.log("did-start-loading");
    const el = e.target;
    if (el === null) throw new Error(`el === null`);
    (e.target as WebviewTag).executeJavaScript(ecxuteJavascriptCode);
  }

  onAnimationend(event: AnimationEvent) {
    this.dispatchEvent(
      new CustomEvent("animationend", {
        bubbles: true,
        detail: {
          customWebview: this.customWebview,
          event: event,
          from: event.target,
        },
      })
    );
  }

  /**
   * 向内部的 webview 的内容执行 code
   * @param code
   */
  executeJavascript = (code: string) => {
    if (this.elWebview === undefined)
      throw new Error(`this.elWebview === undefined`);
    this.elWebview.executeJavaScript(code);
  };

  onPluginNativeUiLoadBase(e: Event) {
    const iframe = e.target as HTMLIFrameElement;
    const contentWindow = iframe.contentWindow as Window;
    // 把 status-bar 添加到容器上
    // 把容器 元素传递给内部的内部的window
    const container = iframe.parentElement as HTMLDivElement;
    Reflect.set(contentWindow, "parentElement", container);
    contentWindow.postMessage("loaded", "*");
  }

  getWebviewTag() {
    return this.elWebview;
  }

  override render() {
    const containerStyleMap = styleMap({
      "--z-index": this.zIndex + "",
      "--scale": this.scale + "",
      "--opacity": this.opacity + "",
    });

    // useragent=${navigator.userAgent + "dweb-host/" + location.host}
    return html`
      <webview
        nodeintegration
        nodeintegrationinsubframes
        allowpopups
        disablewebsecurity
        plugins
        class="webview ${this.closing
          ? `closing-ani-view`
          : `opening-ani-view`}"
        style="${containerStyleMap}"
        @animationend=${this.onAnimationend}
        data-app-url=${this.src}
        id="view-${this.customWebviewId}"
        class="webview"
        src=${ifDefined(this.src)}
        partition="trusted"
        allownw
        preload=${this.preload}
        @dom-ready=${this.onDomReady}
        @did-start-loading=${this.webviewDidStartLoading}
        useragent=${navigator.userAgent + " dweb-host/" + location.host}
      ></webview>
    `;
  }
}

function createAllCSS() {
  return [
    css`
      :host {
        box-sizing: border-box;
        margin: 0px;
        padding: 0px;
        width: 100%;
        height: 100%;
      }

      .webview {
        position: relative;
        box-sizing: border-box;
        width: 100%;
        min-height: 100%;
        scrollbar-width: 2px;
        overflow: hidden;
        overflow-y: auto;
      }
    `,
    // 需要啊全部的custom.属性传递进来
    // 动画相关
    css`
      :host {
        --easing: cubic-bezier(0.36, 0.66, 0.04, 1);
      }
      .opening-ani-view {
        animation: slideIn 520ms var(--easing) forwards;
      }
      .closing-ani-view {
        animation: slideOut 830ms var(--easing) forwards;
      }
      @keyframes slideIn {
        0% {
          transform: translateY(60%) translateZ(0);
          opacity: 0.4;
        }
        100% {
          transform: translateY(0%) translateZ(0);
          opacity: 1;
        }
      }
      @keyframes slideOut {
        0% {
          transform: translateY(0%) translateZ(0);
          opacity: 1;
        }
        30% {
          transform: translateY(-30%) translateZ(0) scale(0.4);
          opacity: 0.6;
        }
        100% {
          transform: translateY(-100%) translateZ(0) scale(0.3);
          opacity: 0.5;
        }
      }
    `,
  ];
}

export interface CustomEventDomReadyDetail {
  customWebview: Webview;
  event: Event;
  from: EventTarget & WebviewTag;
}

export interface CustomEventAnimationendDetail {
  customWebview: Webview;
  event: AnimationEvent;
  from: EventTarget | null;
}
