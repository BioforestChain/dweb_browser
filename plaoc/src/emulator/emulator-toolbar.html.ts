import { css, customElement, html, LitElement, property } from "./helper/litHelper.ts";

const TAG = "emulator-toolbar";

@customElement(TAG)
export class EmulatorToolbarElement extends LitElement {
  static override styles = createAllCSS();
  @property({ type: String }) url = "";

  private _on_keydown_reload = (e: KeyboardEvent) => {
    //键盘按键控制
    e = e || window.event;
    if (
      (e.ctrlKey && e.keyCode == 82) || //ctrl+R
      e.keyCode == 116
    ) {
      debugger;
      //F5刷新，禁止
    }
  };

  override connectedCallback(): void {
    super.connectedCallback();
    document.addEventListener("keydown", this._on_keydown_reload);
  }

  override disconnectedCallback(): void {
    super.disconnectedCallback();
    document.removeEventListener("keydown", this._on_keydown_reload);
  }

  protected override render() {
    return html`
      <div class="bar">
        <input
          .value=${this.url}
          readonly
          @input=${(e: InputEvent & { target: HTMLInputElement }) => {
            this.url = e.target.value;
          }}
        />
      </div>
    `;
  }
}

function createAllCSS() {
  return [
    css`
      :host {
        display: block;
      }
      .bar {
        background: #00000033;
      }
      input {
        width: 100%;
        height: 2em;
      }
    `,
  ];
}

declare global {
  interface HTMLElementTagNameMap {
    [TAG]: EmulatorToolbarElement;
  }
}
