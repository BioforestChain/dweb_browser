import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { styleMap } from "lit/directives/style-map.js";
import { Webview } from "./multi-webview.ts";

import WebviewTag = Electron.WebviewTag;

const allCss = [
  css`
    :host {
      width: 100%;
      height: 100%;
    }

    .container {
      width: 100%;
      height: 100%;
    }

    .toolbar {
      display: flex;
      justify-content: flex-start;
      align-items: flex-start;
      width: 100%;
      height: 60px;
    }

    .devtool {
      width: 100%;
      height: calc(100% - 60px);
      border: 1px solid #ddd;
    }
  `,
  // 需要啊全部的custom.属性传递进来
  // 动画相关
  css`
    :host {
      --easing: cubic-bezier(0.36, 0.66, 0.04, 1);
    }
    .opening-ani-devtools {
      animation: slideIn 520ms var(--easing) forwards;
    }
    .closing-ani-devtools {
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

@customElement("multi-webview-devtools")
export class MultiWebviewDevtools extends LitElement {
  @property({ type: Webview }) customWebview: Webview | undefined = undefined;
  @property({ type: Boolean }) closing: boolean = false;
  @property({ type: Number }) zIndex: Number = 0;
  @property({ type: Number }) scale: Number = 0;
  @property({ type: Number }) opacity: Number = 1;
  @property({ type: Number }) customWebviewId: Number = 0;

  static override styles = allCss;

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
  }

  onDestroy() {
    this.dispatchEvent(new Event("destroy-webview"));
  }

  override render() {
    const containerStyleMap = styleMap({
      "--z-index": this.zIndex + "",
      "--scale": this.scale + "",
      "--opacity": this.opacity + "",
    });

    return html`
      <div
        class="container ${this.closing
          ? "closing-ani-devtools"
          : "opening-ani-devtools"}"
        style=${containerStyleMap}
      >
        <div class="toolbar">
          <button @click=${this.onDestroy}>销毁</button>
        </div>
        <webview
          id="tool-${this.customWebviewId}"
          class="devtool"
          src="about:blank"
          partition="trusted"
          @dom-ready=${this.onDomReady}
        ></webview>
      </div>
    `;
  }
}
