var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
let MultiWebviewCompShare = class MultiWebviewCompShare extends LitElement {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "_title", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "标题 这里是超长的标题，这里是超长的标题这里是超长的，这里是超长的标题，这里是超长的标题"
        });
        Object.defineProperty(this, "_text", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "文本内容 这里是超长的内容，这里是超长的内容，这里是超长的内容，这里是超长的内容，"
        });
        Object.defineProperty(this, "_link", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "http://www.baidu.com?url="
        });
        Object.defineProperty(this, "_src", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "https://img.tukuppt.com/photo-big/00/00/94/6152bc0ce6e5d805.jpg"
        });
    }
    firstUpdated(_changedProperties) {
        this.shadowRoot?.host.addEventListener('click', this.cancel);
    }
    cancel() {
        this.shadowRoot?.host.remove();
    }
    render() {
        return html `
      <div class="panel">
        <img class="img" src=${this._src}></img>
        <div class="text_container">
          <h2 class="h2">${this._title}</h2>
          <p class="p">${this._text}</p>
          <a class="a" href=${this._link} target="_blank">${this._link}</a>
        </div>
      </div>
    `;
    }
};
Object.defineProperty(MultiWebviewCompShare, "styles", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: createAllCSS()
});
__decorate([
    property({ type: String })
], MultiWebviewCompShare.prototype, "_title", void 0);
__decorate([
    property({ type: String })
], MultiWebviewCompShare.prototype, "_text", void 0);
__decorate([
    property({ type: String })
], MultiWebviewCompShare.prototype, "_link", void 0);
__decorate([
    property({ type: String })
], MultiWebviewCompShare.prototype, "_src", void 0);
MultiWebviewCompShare = __decorate([
    customElement('multi-webview-comp-share')
], MultiWebviewCompShare);
export { MultiWebviewCompShare };
function createAllCSS() {
    return [
        css `
      :host{
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

      .panel{
        display: flex;
        flex-direction: column;
        justify-content: center;
        width: 70%;
        border-radius: 6px;
        background: #FFFFFFFF;
        border-radius: 6px;
        overflow: hidden;
      }

      .img{
        display: block;
        box-sizing: border-box;
        padding: 30px;
        max-width: 100%;
        max-height: 300px;
      }

      .text_container{
        box-sizing: border-box;
        padding: 20px;
        width: 100%;
        height: auto;
        background: #000000FF;
      }

      .h2{
        margin: 0px;
        padding: 0px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 16px;
        color: #fff;
      }

      .p{
        margin: 0px;
        padding: 0px;
        font-size: 13px;
        color: #666;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .a{
        display: block;
        font-size: 12px;
        color: #999;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    `
    ];
}
