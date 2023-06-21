/// <reference lib="dom"/>
/// <reference path="../../../../node_modules/vite/client.d.ts"/>
// 效果 webview 容器
import { css, html, LitElement, unsafeCSS } from "lit";
import { customElement, state } from "lit/decorators.js";
import { mainApis } from "../../../helper/openNativeWindow.preload.ts";
//@ts-ignore
import close_src from "./static/close.svg?url";
//@ts-ignore
import minimize_src from "./static/minimize.svg?url";
//@ts-ignore
import fullscreen_src from "./static/fullscreen.svg?url";
//@ts-ignore
import fullscreen_exit_src from "./static/fullscreen-exit.svg?url";

const TAG = "mwebview-title-bar";

@customElement(TAG)
export class TitleBarElement extends LitElement {
  static styles = createAllCSS();
  @state() isFullscreen = mainApis.closeDevTools;
  override render() {
    // useragent=${navigator.userAgent + "dweb-host/" + location.host}
    return html` <nav class="nav">
      <button class="item btn-close" title="close">
        <span class="icon"></span>
      </button>
      <button class="item btn-minimize" title="minimize">
        <span class="icon"></span>
      </button>
      <button class="item btn-fullscreen" title="fullscreen">
        <span class="icon"></span>
      </button>
      <button class="item btn-fullscreen-exit" title="fullscreen-exit">
        <span class="icon"></span>
      </button>
    </nav>`;
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
        -webkit-app-region: drag;
      }
      .nav {
        display: flex;
        flex-direction: row;
        gap: 0.8em;
        padding: 0.5em;
        padding-left: 1em;
        background: rgba(0, 0, 0, 0.6);
        backdrop-filter: blur(5px);
        border: 1px solid rgb(255 255 255 / 50%);
        box-sizing: border-box;
        border-radius: 1em;
        overflow: hidden;
      }
      .item {
        display: inline-block;
        width: 1em;
        height: 1em;
        flex-shrink: 0;
        border-radius: 50%;
        border: none;
        overflow: hidden;
        padding: 0.1em;
        -webkit-app-region: no-drag;
        cursor: pointer;
      }
      .item > .icon {
        background-image: var(--icon-url);
        background-size: 100% 100%;
        background-repeat: no-repeat;
        opacity: 0;
        transition-duration: 0.6s;
        transition-timing-function: ease-out;
        display: block;
        height: 100%;
        width: 100%;
      }
      .item:hover > .icon {
        opacity: 1;
      }
      .btn-close {
        background-color: red;
        --icon-url: url(${unsafeCSS(close_src)});
      }
      .btn-minimize {
        background-color: yellow;
        --icon-url: url(${unsafeCSS(minimize_src)});
      }
      .btn-fullscreen {
        background-color: green;
        --icon-url: url(${unsafeCSS(fullscreen_src)});
      }
      .btn-fullscreen-exit {
        background-color: green;
        --icon-url: url(${unsafeCSS(fullscreen_exit_src)});
      }
    `,
  ];
}

declare global {
  interface HTMLElementTagNameMap {
    [TAG]: TitleBarElement;
  }
}
