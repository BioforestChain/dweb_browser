import { css, customElement, html, LitElement, property } from "./helper/litHelper.ts";

const TAG = "multi-webview-comp-haptics";

@customElement(TAG)
export class MultiWebviewCompHaptics extends LitElement {
  static override styles = createAllCSS();

  @property({ type: String }) text = "";

  override firstUpdated() {
    this.shadowRoot?.host.addEventListener("click", this.cancel);
  }

  cancel() {
    this.shadowRoot?.host.remove();
  }

  protected override render(): unknown {
    return html`
      <div class="panel">
        <p>模拟: ${this.text}</p>
        <div class="btn_group">
          <button class="btn" @click=${this.cancel}>取消</button>
        </div>
      </div>
    `;
  }
}

function createAllCSS() {
  return [
    css`
      :host {
        position: absolute;
        z-index: 1;
        left: 0px;
        top: 0px;
        box-sizing: border-box;
        padding-bottom: 100px;
        width: 100%;
        height: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
        background: #00000033;
        cursor: pointer;
      }

      .panel {
        padding: 12px 20px;
        width: 80%;
        border-radius: 12px;
        background: #ffffffff;
      }

      .btn_group {
        width: 100%;
        display: flex;
        justify-content: flex-end;
      }

      .btn {
        padding: 8px 20px;
        border-radius: 5px;
        border: none;
        color: #ffffffff;
        background: #1677ff;
      }
    `,
  ];
}

declare global {
  interface HTMLElementTagNameMap {
    [TAG]: MultiWebviewCompHaptics;
  }
}
