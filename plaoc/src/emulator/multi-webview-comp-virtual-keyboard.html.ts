import { classMap, css, customElement, html, LitElement, property, query, repeat } from "./helper/litHelper.ts";

const TAG = "multi-webview-comp-virtual-keyboard";

@customElement(TAG)
export class MultiWebviewCompVirtualKeyboard extends LitElement {
  static override styles = createAllCSS();
  @query(".container") _elContainer: HTMLDivElement | undefined;
  @property({ type: Boolean }) _visible = false;
  @property({ type: Boolean }) _overlay = false;
  @property({ type: Number }) _navigation_bar_height = 0;
  timer = 0;
  requestId = 0;
  insets = {
    left: 0,
    top: 0,
    right: 0,
    bottom: 0,
  };
  maxHeight = 0;
  row1Keys = ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"];
  row2Keys = ["a", "s", "d", "f", "g", "h", "j", "k", "l"];
  row3Keys = ["&#8679", "z", "x", "c", "v", "b", "n", "m", "&#10005"];
  row4Keys = ["123", "&#128512", "space", "search"];

  setHostStyle() {
    const host = (this.renderRoot as ShadowRoot).host as HTMLElement;
    host.style.position = this._overlay ? "absolute" : "relative";
    host.style.overflow = this._visible ? "visible" : "hidden";
  }

  override firstUpdated() {
    this.setCSSVar();
    this.dispatchEvent(new Event("first-updated"));
  }

  setCSSVar() {
    if (!this._elContainer) throw new Error(`this._elContainer === null`);
    const rowWidth = this._elContainer.getBoundingClientRect().width;
    const alphabetWidth = rowWidth / 11;
    const alphabetHeight = alphabetWidth * 1;
    const rowPaddingVertical = 3;
    const rowPaddingHorizontal = 2;
    this.maxHeight = (alphabetHeight + rowPaddingVertical * 2) * 4 + alphabetHeight;
    [
      ["--key-alphabet-width", alphabetWidth],
      ["--key-alphabet-height", alphabetHeight],
      ["--row-padding-vertical", rowPaddingVertical],
      ["--row-padding-horizontal", rowPaddingHorizontal],
      ["--height", this._navigation_bar_height],
    ].forEach(([propertyName, n]) => {
      this._elContainer?.style.setProperty(propertyName as string, n + "px");
    });
    return this;
  }

  repeatGetKey(item: string) {
    return item;
  }

  createElement(classname: string, key: string) {
    const div = document.createElement("div");
    div.setAttribute("class", classname);
    div.innerHTML = key;
    return div;
  }

  createElementForRow3(classNameSymbol: string, classNameAlphabet: string, key: string) {
    return this.createElement(key.startsWith("&") ? classNameSymbol : classNameAlphabet, key);
  }

  createElementForRow4(classNameSymbol: string, classNameSpace: string, classNameSearch: string, key: string) {
    return this.createElement(
      key.startsWith("1") || key.startsWith("&") ? classNameSymbol : key === "space" ? classNameSpace : classNameSearch,
      key
    );
  }

  transitionstart() {
    this.timer = setInterval(() => {
      this.dispatchEvent(new Event("height-changed"));
    }, 16);
  }

  transitionend() {
    this.dispatchEvent(new Event(this._visible ? "show-completed" : "hide-completed"));
    clearInterval(this.timer as number);
    this.dispatchEvent(new Event("height-changed"));
  }

  protected override render(): unknown {
    this.setHostStyle();
    const containerClassMap = {
      container: true,
      container_active: this._visible,
    };
    return html`
      <div
        class="${classMap(containerClassMap)}"
        @transitionstart=${this.transitionstart}
        @transitionend=${this.transitionend}
      >
        <div class="row line-1">
          ${repeat(this.row1Keys, this.repeatGetKey, this.createElement.bind(this, "key-alphabet"))}
        </div>
        <div class="row line-2">
          ${repeat(this.row2Keys, this.repeatGetKey, this.createElement.bind(this, "key-alphabet"))}
        </div>
        <div class="row line-3">
          ${repeat(
            this.row3Keys,
            this.repeatGetKey,
            this.createElementForRow3.bind(this, "key-symbol", "key-alphabet")
          )}
        </div>
        <div class="row line-4">
          ${repeat(
            this.row4Keys,
            this.repeatGetKey,
            this.createElementForRow4.bind(this, "key-symbol", "key-space", "key-search")
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
        left: 0px;
        bottom: 0px;
        width: 100%;
      }

      .container {
        --key-alphabet-width: 0px;
        --key-alphabet-height: 0px;
        --row-padding-vertical: 3px;
        --row-padding-horizontal: 2px;
        --border-radius: 3px;
        --height: 0px;
        margin: 0px;
        height: var(--height);
        transition: all 0.25s ease-out;
        overflow: hidden;
        background: #999999;
      }

      .container_active {
        height: calc((var(--key-alphabet-height) + var(--row-padding-vertical) * 2) * 4 + var(--key-alphabet-height));
      }

      .row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: var(--row-padding-vertical) var(--row-padding-horizontal);
      }

      .key-alphabet {
        display: flex;
        justify-content: center;
        align-items: center;
        width: var(--key-alphabet-width);
        height: var(--key-alphabet-height);
        border-radius: var(--border-radius);
        background: #fff;
      }

      .line-2 {
        padding: var(--row-padding-vertical) calc(var(--row-padding-horizontal) + var(--key-alphabet-width) / 2);
      }

      .key-symbol {
        --margin-horizontal: calc(var(--key-alphabet-width) * 0.3);
        display: flex;
        justify-content: center;
        align-items: center;
        width: calc(var(--key-alphabet-width) * 1.2);
        height: var(--key-alphabet-height);
        border-radius: var(--border-radius);
        background: #aaa;
      }

      .key-symbol:first-child {
        margin-right: var(--margin-horizontal);
      }

      .key-symbol:last-child {
        margin-left: var(--margin-horizontal);
      }

      .line-4 .key-symbol:first-child {
        margin-right: 0px;
      }

      .line-4 .key-symbol:nth-of-type(2) {
        width: calc(var(--key-alphabet-width) * 1.3);
      }

      .key-space {
        display: flex;
        justify-content: center;
        align-items: center;
        border-radius: var(--border-radius);
        width: calc(var(--key-alphabet-width) * 6);
        height: var(--key-alphabet-height);
        background: #fff;
      }

      .key-search {
        width: calc(var(--key-alphabet-width) * 2);
        height: var(--key-alphabet-height);
        display: flex;
        justify-content: center;
        align-items: center;
        border-radius: var(--border-radius);
        background: #4096ff;
        color: #fff;
      }
    `,
  ];
}
