/// <reference lib="dom"/>
// 效果 webview 容器
import { css, html, LitElement } from "lit";
import { customElement, property, query, state } from "lit/decorators.js";
import { ifDefined } from "lit/directives/if-defined.js";
import { styleMap } from "lit/directives/style-map.js";
import ecxuteJavascriptCode from "./viewItem.domReady.ts";
import WebviewTag = Electron.WebviewTag;

const TAG = "mwebview-view-item";

@customElement(TAG)
export class MultiWebViewContent extends LitElement {
  static styles = createAllCSS();

  @property({ type: Boolean }) closing = false;
  @property({ type: Number }) zIndex = 0;
  @property({ type: Number }) scale = 0;
  @property({ type: Number }) opacity = 1;
  @property({ type: Number }) customWebviewId = 0;
  @property({ type: String }) src = "";
  @property({ type: String }) preload = "";
  @state() statusbarHidden = false;
  @query("webview") elWebview!: WebviewTag;

  onDomReady(event: Event) {
    this.dispatchEvent(
      new CustomEvent<CustomEventDomReadyDetail>("dom-ready", {
        bubbles: true,
        detail: {
          event: event,
          from: this.elWebview,
        },
      })
    );
    this.webviewDidStartLoading(event);
  }

  webviewDidStartLoading(e: Event) {
    const el = e.target;
    if (el === null) throw new Error(`el === null`);
    (e.target as WebviewTag).executeJavaScript(
      ecxuteJavascriptCode.toString().match(/\{([\w\W]+)\}/)![1]
    );
  }

  onAnimationend(event: AnimationEvent) {
    this.dispatchEvent(
      new CustomEvent("animationend", {
        bubbles: true,
        detail: {
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
  event: Event;
  from: EventTarget & WebviewTag;
}

export interface CustomEventAnimationendDetail {
  event: AnimationEvent;
  from: EventTarget | null;
}

declare global {
  interface HTMLElementTagNameMap {
    [TAG]: MultiWebViewContent;
  }
}
