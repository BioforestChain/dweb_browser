import { LitElement, css, html } from "lit";
import { customElement } from "lit/decorators.js";
import { icons } from "./icons/index.ts";

import { importApis } from "../../../../helper/openNativeWindow.preload.ts";
const mainApis = importApis();

const TAG = "bw-taskbar";
@customElement(TAG)
export class TaskbarElement extends LitElement {
  queryApp() {}
  openApp() {}
  static override styles = [
    css`
      :host {
        display: block;
        height: min-content;
        width: min-content;
        -webkit-app-region: drag;
      }
      .panel {
        padding: 1em;
        cursor: move;
      }
      .app-icon {
        width: 60px;
        height: 60px;
        border-radius: 15px;
        background-color: #fff;
        display: flex;
        justify-content: center;
        align-items: center;
        box-shadow: 0px 3px 5px rgba(0, 0, 0, 0.3);
        .img {
          width: 90%;
          height: auto;
        }
      }
    `,
  ];
  override render() {
    return html`
      <div class="panel">
        <div class="app-icon">
          <img class="img" src=${icons.anquanzhongxin} />
        </div>
        <div class="app-icon">
          <img class="img" src=${icons.kandianying} />
        </div>
        <div class="app-icon">
          <img class="img" src=${icons.naozhong} />
        </div>
        <div class="app-icon">
          <img class="img" src=${icons.xiangji} />
        </div>
        <div class="app-icon">
          <img class="img" src=${icons.quanbufenlei} />
        </div>
      </div>
    `;
  }
}

declare global {
  interface HTMLElementTagNameMap {
    [TAG]: TaskbarElement;
  }
}
