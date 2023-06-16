import { css, html, LitElement, PropertyValueMap } from "lit";
import { customElement, property } from "lit/decorators.js";
import { styleMap } from "lit/directives/style-map.js";
import { hexaToRGBA } from "../../../../helper/color.ts";
const { ipcRenderer } = Electron;

@customElement("multi-webview-comp-navigation-bar")
export class MultiWebviewCompNavigationBar extends LitElement {
  static override styles = createAllCSS();

  @property({ type: String }) _color = "#ccccccFF";
  @property({ type: String }) _style = "DEFAULT";
  @property({ type: Boolean }) _overlay = false;
  @property({ type: Boolean }) _visible = true;
  @property({ type: Object }) _insets = {
    top: 0,
    right: 0,
    bottom: 20,
    left: 0,
  };
  @property() _webview_src: any = {};

  protected override updated(
    _changedProperties: PropertyValueMap<any> | Map<PropertyKey, unknown>
  ): void {
    const attributes = Array.from(_changedProperties.keys());
    ipcRenderer.send(
      "navigation_bar_state_change",
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
    if (attributes.includes("_visible") || attributes.includes("_overlay")) {
      this.dispatchEvent(new Event("safe_area_need_update"));
    }
  }

  createBackgroundStyleMap() {
    return {
      backgroundColor: this._overlay ? "transparent" : this._color,
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

  setHostStyle() {
    const host = (this.renderRoot as ShadowRoot).host as HTMLElement;
    host.style.position = this._overlay ? "absolute" : "relative";
    host.style.overflow = this._visible ? "visible" : "hidden";
  }

  back() {
    this.dispatchEvent(new Event("back"));
  }

  home() {
    console.error("navigation-bar click home 但是还没有处理");
  }

  menu() {
    console.error(`navigation-bar 点击了menu 但是还没有处理`);
  }

  override render() {
    this.setHostStyle();
    const backgroundStyleMap = this.createBackgroundStyleMap();
    const containerStyleMap = this.createContainerStyleMap();
    return html`
      <div class="container">
        <div class="background" style=${styleMap(backgroundStyleMap)}></div>
        <!-- android 导航栏 -->
        <div
          class="navigation_bar_container"
          style=${styleMap(containerStyleMap)}
        >
          <div class="menu" @click="${this.menu}">
            <svg
              class="icon_svg menu_svg"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 448 512"
            >
              <path
                fill="currentColor"
                d="M0 96C0 78.3 14.3 64 32 64H416c17.7 0 32 14.3 32 32s-14.3 32-32 32H32C14.3 128 0 113.7 0 96zM0 256c0-17.7 14.3-32 32-32H416c17.7 0 32 14.3 32 32s-14.3 32-32 32H32c-17.7 0-32-14.3-32-32zM448 416c0 17.7-14.3 32-32 32H32c-17.7 0-32-14.3-32-32s14.3-32 32-32H416c17.7 0 32 14.3 32 32z"
              />
            </svg>
          </div>
          <div class="home" @click="${this.home}">
            <svg
              class="icon_svg"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 512 512"
            >
              <path
                fill="currentColor"
                d="M464 256A208 208 0 1 0 48 256a208 208 0 1 0 416 0zM0 256a256 256 0 1 1 512 0A256 256 0 1 1 0 256z"
              />
            </svg>
          </div>
          <div class="back" @click="${this.back}">
            <svg
              class="icon_svg"
              viewBox="0 0 1024 1024"
              version="1.1"
              xmlns="http://www.w3.org/2000/svg"
            >
              <path
                fill="currentColor"
                d="M814.40768 119.93088a46.08 46.08 0 0 0-45.13792 2.58048l-568.07424 368.64a40.42752 40.42752 0 0 0-18.75968 33.71008c0 13.39392 7.00416 25.96864 18.75968 33.66912l568.07424 368.64c13.35296 8.68352 30.72 9.66656 45.13792 2.58048a40.67328 40.67328 0 0 0 23.38816-36.2496v-737.28a40.71424 40.71424 0 0 0-23.38816-36.29056zM750.3872 815.3088L302.81728 524.86144l447.61088-290.44736v580.89472z"
              ></path>
            </svg>
          </div>
        </div>
      </div>
    `;
  }
}

function createAllCSS() {
  return [
    css`
      :host {
        position: relative;
        z-index: 999999999;
        box-sizing: border-box;
        left: 0px;
        bottom: 0px;
        margin: 0px;
        width: 100%;
        -webkit-app-region: drag;
        -webkit-user-select: none;
      }

      .container {
        position: relative;
        box-sizing: border-box;
        width: 100%;
        height: 26px;
      }
      .background {
        position: absolute;
        top: 0px;
        left: 0px;
        width: 100%;
        height: 100%;
        background: #ffffff00;
      }

      .line-container {
        position: absolute;
        top: 0px;
        left: 0px;
        display: flex;
        justify-content: center;
        align-items: center;
        width: 100%;
        height: 100%;
      }

      .line {
        width: 50%;
        height: 4px;
        border-radius: 4px;
      }

      .line-default {
        background: #ffffffff;
      }

      .line-dark {
        background: #000000ff;
      }

      .line-light {
        background: #ffffffff;
      }

      .navigation_bar_container {
        position: absolute;
        top: 0px;
        left: 0px;
        display: flex;
        justify-content: space-around;
        align-items: center;
        width: 100%;
        height: 100%;
      }

      .menu,
      .home,
      .back {
        display: flex;
        justify-content: center;
        align-items: center;
        cursor: pointer;
        -webkit-app-region: no-drag;
      }

      .icon_svg {
        width: 20px;
        height: 20px;
      }
    `,
  ];
}
