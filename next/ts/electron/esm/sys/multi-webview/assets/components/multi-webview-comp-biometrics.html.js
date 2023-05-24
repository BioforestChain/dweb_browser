var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, LitElement } from "lit";
import { customElement } from "lit/decorators.js";
import { html } from "lit/static-html.js";
let MultiWebviewCompBiometrics = class MultiWebviewCompBiometrics extends LitElement {
    pass() {
        console.error('点击了 pass 但是还没有处理');
        this.dispatchEvent(new Event('pass'));
        this.shadowRoot?.host.remove();
    }
    noPass() {
        console.error('点击了 no pass 但是还没有处理');
        this.dispatchEvent(new Event("no-pass"));
        this.shadowRoot?.host.remove();
    }
    render() {
        return html `
      <div class="panel">
        <p>点击按钮 模拟 返回结果</p>
        <div class="btn_group">
          <button class="pass" @click=${this.pass}>识别通过</button>
          <button class="no_pass" @click=${this.noPass}>识别没通过</button>
        </div>
      </div>
    `;
    }
};
Object.defineProperty(MultiWebviewCompBiometrics, "styles", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: createAllCSS()
});
MultiWebviewCompBiometrics = __decorate([
    customElement('multi-webview-comp-biometrics')
], MultiWebviewCompBiometrics);
export { MultiWebviewCompBiometrics };
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
        justify-content: space-between;
      }

      .pass,
      .no_pass{
        padding: 8px 20px;
        border-radius: 5px;
        border: none;

      }

      .pass{
        color: #FFFFFFFF;
        background: #1677ff;    
      }

      .no_pass{
        background: #d9d9d9;
      }
    `
    ];
}
