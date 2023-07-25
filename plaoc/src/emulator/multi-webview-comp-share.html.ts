import { css, customElement, html, LitElement, property, PropertyValueMap, when } from "./helper/litHelper.ts";

const TAG = "multi-webview-comp-share";

@customElement(TAG)
export class MultiWebviewCompShare extends LitElement {
  static override styles = createAllCSS();

  @property({ type: String }) _title =
    "标题 这里是超长的标题，这里是超长的标题这里是超长的，这里是超长的标题，这里是超长的标题";
  @property({ type: String }) _text =
    "文本内容 这里是超长的内容，这里是超长的内容，这里是超长的内容，这里是超长的内容，";
  @property({ type: String }) _link = "http://www.baidu.com?url=";
  @property({ type: String }) _src = "https://img.tukuppt.com/photo-big/00/00/94/6152bc0ce6e5d805.jpg";
  @property({ type: String }) _filename = "";
  protected override firstUpdated(_changedProperties: PropertyValueMap<any> | Map<PropertyKey, unknown>): void {
    this.shadowRoot?.host.addEventListener("click", this.cancel);
  }

  cancel() {
    this.shadowRoot?.host.remove();
  }

  override render() {
    console.log("this._src: ", this._src);
    return html`
      <div class="panel">
        ${when(
          this._src,
          () => html`<img class="img" src=${this._src} />`,
          () => html`<div class="filename">${this._filename}</div>`
        )}
        <div class="text_container">
          <h2 class="h2">${this._title}</h2>
          <p class="p">${this._text}</p>
          <a class="a" href=${this._link} target="_blank">${this._link}</a>
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
        padding-bottom: 200px;
        width: 100%;
        height: 100%;
        display: flex;
        justify-content: center;
        align-items: center;
        background: #000000cc;
        cursor: pointer;
        backdrop-filter: blur(5px);
      }

      .panel {
        display: flex;
        flex-direction: column;
        justify-content: center;
        width: 70%;
        border-radius: 6px;
        background: #ffffffff;
        border-radius: 6px;
        overflow: hidden;
      }

      .img {
        display: block;
        box-sizing: border-box;
        padding: 30px;
        max-width: 100%;
        max-height: 300px;
      }

      .filename {
        display: flex;
        justify-content: center;
        align-items: center;
        box-sizing: border-box;
        padding: 30px;
        max-width: 100%;
        max-height: 300px;
        font-size: 18px;
      }

      .text_container {
        box-sizing: border-box;
        padding: 20px;
        width: 100%;
        height: auto;
        background: #000000ff;
      }

      .h2 {
        margin: 0px;
        padding: 0px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 16px;
        color: #fff;
      }

      .p {
        margin: 0px;
        padding: 0px;
        font-size: 13px;
        color: #666;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .a {
        display: block;
        font-size: 12px;
        color: #999;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    `,
  ];
}

declare global {
  interface HTMLElementTagNameMap {
    [TAG]: MultiWebviewCompShare;
  }
}
