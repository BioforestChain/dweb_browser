// 状态栏
import { css, html, LitElement, PropertyValueMap } from "lit";
import { customElement, property } from "lit/decorators.js";
import { styleMap } from "lit/directives/style-map.js";
import { when } from "lit/directives/when.js";
import { hexaToRGBA } from "../../../../helper/color.ts";
const { ipcRenderer } = Electron;

@customElement("multi-webview-comp-status-bar")
export class MultiWebviewCompStatusBar extends LitElement {
  @property({ type: String }) _color = "#FFFFFFFF";
  @property({ type: String }) _style = "DEFAULT";
  @property({ type: Boolean }) _overlay = false;
  @property({ type: Boolean }) _visible = true;
  @property({ type: String }) _height = "38px";
  @property({ type: Object }) _insets = {
    top: this._height,
    right: 0,
    bottom: 0,
    left: 0,
  };
  @property({ type: Boolean }) _torchIsOpen = false;
  @property() _webview_src: any = {};

  static override styles = createAllCSS();

  createBackgroundStyleMap() {
    return {
      backgroundColor: this._visible
        ? this._overlay
          ? "transparent"
          : this._color
        : "#000000FF",
    };
  }

  createContainerStyleMap() {
    const isLight = window.matchMedia("(prefers-color-scheme: light)");
    return {
      color:
        this._style === "LIGHT"
          ? "#000000FF"
          : this._style === "DARK"
          ? "#FFFFFFFF"
          : isLight
          ? "#000000FF"
          : "#FFFFFFFF",
    };
  }

  protected override updated(
    _changedProperties: PropertyValueMap<any> | Map<PropertyKey, unknown>
  ): void {
    const attributeProperties = Array.from(_changedProperties.keys());
    ipcRenderer.send(
      "status_bar_state_change",
      // 数据格式 api.browser.dweb-443.localhost:22605
      new URL(this._webview_src).host.replace("www.", "api."),
      {
        color: hexaToRGBA(this._color),
        style: this._style,
        overlay: this._overlay,
        visible: this._visible,
        insets: this._insets,
      }
    );

    // 在影响 safe-area 的情况下 需要报消息发送给 safe-area 模块
    if (
      attributeProperties.includes("_visible") ||
      attributeProperties.includes("_overlay")
    ) {
      this.dispatchEvent(new Event("safe_area_need_update"));
    }
  }

  setHostStyle() {
    const host = (this.renderRoot as ShadowRoot).host as HTMLElement;
    host.style.position = this._overlay ? "absolute" : "relative";
    host.style.overflow = this._visible ? "visible" : "hidden";
  }

  protected override render(): unknown {
    this.setHostStyle();
    const backgroundStyleMap = this.createBackgroundStyleMap();
    const containerStyleMap = this.createContainerStyleMap();
    return html`
      <div class="comp-container">
        <div class="background" style=${styleMap(backgroundStyleMap)}></div>
        <div class="container" style=${styleMap(containerStyleMap)}>
          ${when(
            this._visible,
            () => html`<div class="left_container">10:00</div>`,
            () => html``
          )}
          <div class="center_container">
            ${when(
              this._torchIsOpen,
              () => html`<div class="torch_symbol"></div>`,
              () => html``
            )}
          </div>
          ${when(
            this._visible,
            () => html`
              <div class="right_container">
                <!-- 移动信号标志 -->
                <svg
                  t="1677291966287"
                  class="icon icon-signal"
                  viewBox="0 0 1024 1024"
                  version="1.1"
                  xmlns="http://www.w3.org/2000/svg"
                  p-id="5745"
                  width="32"
                  height="32"
                >
                  <path
                    fill="currentColor"
                    d="M0 704h208v192H0zM272 512h208v384H272zM544 288h208v608H544zM816 128h208v768H816z"
                    p-id="5746"
                  ></path>
                </svg>

                <!-- wifi 信号标志 -->
                <svg
                  t="1677291873784"
                  class="icon icon-wifi"
                  viewBox="0 0 1024 1024"
                  version="1.1"
                  xmlns="http://www.w3.org/2000/svg"
                  p-id="4699"
                  width="32"
                  height="32"
                >
                  <path
                    fill="currentColor"
                    d="M512 896 665.6 691.2C622.933333 659.2 569.6 640 512 640 454.4 640 401.066667 659.2 358.4 691.2L512 896M512 128C339.2 128 179.626667 185.173333 51.2 281.6L128 384C234.666667 303.786667 367.786667 256 512 256 656.213333 256 789.333333 303.786667 896 384L972.8 281.6C844.373333 185.173333 684.8 128 512 128M512 384C396.8 384 290.56 421.973333 204.8 486.4L281.6 588.8C345.6 540.586667 425.386667 512 512 512 598.613333 512 678.4 540.586667 742.4 588.8L819.2 486.4C733.44 421.973333 627.2 384 512 384Z"
                    p-id="4700"
                  ></path>
                </svg>

                <!-- 电池电量标志 -->
                <svg
                  t="1677291736404"
                  class="icon icon-electricity"
                  viewBox="0 0 1024 1024"
                  version="1.1"
                  xmlns="http://www.w3.org/2000/svg"
                  p-id="2796"
                  width="32"
                  height="32"
                >
                  <path
                    fill="currentColor"
                    d="M984.2 434.8c-5-2.9-8.2-8.2-8.2-13.9v-99.3c0-53.6-43.9-97.5-97.5-97.5h-781C43.9 224 0 267.9 0 321.5v380.9C0 756.1 43.9 800 97.5 800h780.9c53.6 0 97.5-43.9 97.5-97.5v-99.3c0-5.8 3.2-11 8.2-13.9 23.8-13.9 39.8-39.7 39.8-69.2v-16c0.1-29.6-15.9-55.5-39.7-69.3zM912 702.5c0 12-6.2 19.9-9.9 23.6-3.7 3.7-11.7 9.9-23.6 9.9h-781c-11.9 0-19.9-6.2-23.6-9.9-3.7-3.7-9.9-11.7-9.9-23.6v-381c0-11.9 6.2-19.9 9.9-23.6 3.7-3.7 11.7-9.9 23.6-9.9h780.9c11.9 0 19.9 6.2 23.6 9.9 3.7 3.7 9.9 11.7 9.9 23.6v381z"
                    fill="#606266"
                    p-id="2797"
                  ></path>
                  <path
                    fill="currentColor"
                    d="M736 344v336c0 8.8-7.2 16-16 16H112c-8.8 0-16-7.2-16-16V344c0-8.8 7.2-16 16-16h608c8.8 0 16 7.2 16 16z"
                    fill="#606266"
                    p-id="2798"
                  ></path>
                </svg>
              </div>
            `,
            () => html``
          )}
        </div>
      </div>
    `;
  }
}

function createAllCSS() {
  return [
    css`
      :host {
        z-index: 999999999;
        flex-grow: 0;
        flex-shrink: 0;
        width: 100%;
        height: 38px;
        -webkit-app-region: drag;
        -webkit-user-select: none;
      }

      .comp-container {
        --height: 48px;
        --cell-width: 80px;
        position: relative;
        width: 100%;
        height: 100%;
      }

      // html{
      //   width:100%;
      //   height: var(--height);
      //   overflow: hidden;
      // }

      .background {
        position: absolute;
        left: 0px;
        top: 0px;
        width: 100%;
        height: 100%;
        background: #ffffffff;
      }

      .container {
        position: absolute;
        left: 0px;
        top: 0px;
        display: flex;
        justify-content: center;
        align-items: center;
        width: 100%;
        height: 100%;
      }

      .container-light {
        color: #ffffffff;
      }

      .container-dark {
        color: #000000ff;
      }

      .container-default {
        color: #ffffffff;
      }

      .left_container {
        display: flex;
        justify-content: center;
        align-items: flex-end;
        width: var(--cell-width);
        height: 100%;
        font-size: 15px;
        font-weight: 900;
      }

      .center_container {
        position: relative;
        display: flex;
        justify-content: center;
        align-items: center;
        width: calc(100% - var(--cell-width) * 2);
        height: 100%;
        border-bottom-left-radius: var(--border-radius);
        border-bottom-right-radius: var(--border-radius);
      }

      .center_container::after {
        content: "";
        width: 50%;
        height: 20px;
        border-radius: 10px;
        background: #111111;
      }

      .torch_symbol {
        position: absolute;
        z-index: 1;
        width: 10px;
        height: 10px;
        border-radius: 20px;
        background: #fa541c;
      }

      .right_container {
        display: flex;
        justify-content: flex-start;
        align-items: flex-end;
        width: var(--cell-width);
        height: 100%;
      }

      .icon {
        margin-right: 5px;
        width: 18px;
        height: 18px;
      }
    `,
  ];
}
