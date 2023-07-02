// 状态栏
import { css, html, LitElement, PropertyValueMap } from "lit";
import { customElement, property } from "lit/decorators.js";
import { classMap } from "lit/directives/class-map.js";
import { styleMap } from "lit/directives/style-map.js";
import { when } from "lit/directives/when.js";

const TAG = "multi-webview-comp-status-bar";

@customElement(TAG)
export class MultiWebviewCompStatusBar extends LitElement {
  @property({ type: String }) _color = "#FFFFFF80";
  @property({ type: String }) _style = "DEFAULT";
  @property({ type: Boolean }) _overlay = false;
  @property({ type: Boolean }) _visible = true;
  @property({ type: Object }) _insets = {
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
  };
  @property({ type: Boolean }) _torchIsOpen = false;

  static override styles = createAllCSS();

  protected override updated(
    changedProperties: PropertyValueMap<MultiWebviewCompStatusBar>
  ) {
    // 在影响 safe-area 的情况下 需要报消息发送给 safe-area 模块
    if (
      changedProperties.has("_visible") ||
      changedProperties.has("_overlay")
    ) {
      this.dispatchEvent(new Event("safe_area_need_update"));
    }
    super.updated(changedProperties);
  }

  protected override render() {
    return html`
      <div
        class=${classMap({
          "comp-container": true,
          overlay: this._overlay,
          visible: this._visible,
          [this._style.toLowerCase()]: true,
        })}
        style=${styleMap({
          "--bg-color": this._color,
          height: this._insets.top + "px",
        })}
      >
        <div class="background"></div>
        <div class="container">
          ${when(
            this._visible,
            () => html`<div class="left_container">10:00</div>`
          )}
          <div class="center_container">
            ${when(
              this._torchIsOpen,
              () => html`<div class="torch_symbol"></div>`
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
            `
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
        display: block;
        -webkit-app-region: drag;
        -webkit-user-select: none;
        --cell-width: 80px;
      }

      .comp-container {
        display: grid;
        grid-template-columns: 1fr;
        grid-template-rows: 1fr;
        gap: 0px 0px;
        grid-template-areas: "view";
      }
      .comp-container.overlay {
        position: absolute;
        width: 100%;
        z-index: 1;
      }
      .comp-container:not(.visible) {
        display: none;
      }
      .comp-container.light {
        --fg-color: #ffffffff;
      }
      .comp-container.dark {
        --fg-color: #000000ff;
      }
      .comp-container.default {
        --fg-color: #ffffffff;
      }

      .background {
        grid-area: view;

        background: var(--bg-color);
      }

      .container {
        grid-area: view;
        color: var(--fg-color);

        display: flex;
        justify-content: center;
        align-items: flex-end;

        font-family: PingFangSC-Light, sans-serif;
      }
      /// 使用混合模式自适应当前视图的，大部分情况下可以使用，但是如何状态是灰色，这个效果会很糟糕。对此需要更好的css函数来解决，而不应该依靠js
      .comp-container.default .left_container,
      .comp-container.default .right_container {
        mix-blend-mode: difference;
      }

      .left_container {
        display: flex;
        justify-content: center;
        align-items: center;
        width: var(--cell-width);
        height: 100%;
        font-size: 15px;
        font-weight: 900;
        height: 2em;
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
        align-items: center;
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

declare global {
  interface HTMLElementTagNameMap {
    [TAG]: MultiWebviewCompStatusBar;
  }
}
