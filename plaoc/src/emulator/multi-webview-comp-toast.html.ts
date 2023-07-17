import { classMap, css, customElement, html, LitElement, property, state } from "./helper/litHelper.ts";

const TAG = "multi-webview-comp-toast";

@customElement(TAG)
export class MultiWebviewCompToast extends LitElement {
  static override styles = createAllCSS();
  static override properties = {
    _beforeEntry: { state: true },
  };

  @property({ type: String }) _message = "test message";
  @property({ type: String }) _duration = `1000`;
  @property({ type: String }) _position = "top";
  @state() _beforeEntry = true;

  override firstUpdated() {
    setTimeout(() => {
      this._beforeEntry = false;
    }, 50);
  }

  transitionend(e: TransitionEvent) {
    if (this._beforeEntry) {
      (e.target as HTMLDivElement).remove();
      return;
    }
    setTimeout(
      () => {
        this._beforeEntry = true;
      },
      this._duration === "short" ? 2000 : 3500
    );
  }

  protected override render() {
    const containerClassMap = {
      container: true,
      before_entry: this._beforeEntry ? true : false,
      after_entry: this._beforeEntry ? false : true,
      container_bottom: this._position === "bottom" ? true : false,
      container_top: this._position === "bottom" ? false : true,
    };
    return html`
      <div class=${classMap(containerClassMap)} @transitionend=${this.transitionend}>
        <p class="message">${this._message}</p>
      </div>
    `;
  }
}

function createAllCSS() {
  return [
    css`
      .container {
        position: absolute;
        left: 0px;
        box-sizing: border-box;
        padding: 0px 20px;
        width: 100%;
        transition: all 0.25s ease-in-out;
      }

      .container_bottom {
        bottom: 0px;
      }

      .container_top {
        top: 0px;
      }

      .before_entry {
        transform: translateX(-100vw);
      }

      .after_entry {
        transform: translateX(0vw);
      }

      .message {
        box-sizing: border-box;
        padding: 0px 6px;
        width: 100%;
        height: 38px;
        color: #ffffff;
        line-height: 38px;
        text-align: left;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        background: #eee;
        border-radius: 5px;
        background: #1677ff;
      }
    `,
  ];
}
