var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
let MultiWebviewCompHaptics = class MultiWebviewCompHaptics extends LitElement {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "text", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: ""
        });
    }
    firstUpdated() {
        this.shadowRoot?.host.addEventListener('click', this.cancel);
    }
    cancel() {
        this.shadowRoot?.host.remove();
    }
    render() {
        return html `
      <div class="panel">
        <p>模拟: ${this.text}</p>
        <div class="btn_group">
          <button class="btn" @click=${this.cancel}>取消</button>
        </div>
      </div>
    `;
    }
};
Object.defineProperty(MultiWebviewCompHaptics, "styles", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: createAllCSS()
});
__decorate([
    property({ type: String })
], MultiWebviewCompHaptics.prototype, "text", void 0);
MultiWebviewCompHaptics = __decorate([
    customElement('multi-webview-comp-haptics')
], MultiWebviewCompHaptics);
export { MultiWebviewCompHaptics };
function createAllCSS() {
    return [
        css `
      :host{
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

      .panel{
        padding: 12px 20px;
        width: 80%;
        border-radius: 12px;
        background: #FFFFFFFF;
      }

      .btn_group{
        width: 100%;
        display: flex;
        justify-content: flex-end;
      }
      
      .btn{
        padding: 8px 20px;
        border-radius: 5px;
        border: none;
        color: #FFFFFFFF;
        background: #1677ff; 

      }
    `
    ];
}
