var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, LitElement } from "lit";
import { customElement, query } from "lit/decorators.js";
import { html } from "lit/static-html.js";
let MultiWebviewCompBarcodeScanning = class MultiWebviewCompBarcodeScanning extends LitElement {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "elInput", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
    }
    firstUpdated(_changedProperties) {
        if (this.elInput === undefined || this.elInput === null)
            throw new Error(`this.elInput === undefined || this.elInput === null`);
        this.elInput.click();
    }
    onChange(e) {
        const elInput = e.target;
        console.log('onChange');
        // const file = elInput.files?[0];
    }
    render() {
        return html `
      <input type="file" @change=${this.onChange}/>
    `;
    }
};
Object.defineProperty(MultiWebviewCompBarcodeScanning, "styles", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: createAllCSS()
});
__decorate([
    query("input")
], MultiWebviewCompBarcodeScanning.prototype, "elInput", void 0);
MultiWebviewCompBarcodeScanning = __decorate([
    customElement('multi-webview-comp-barcode-scanning')
], MultiWebviewCompBarcodeScanning);
export { MultiWebviewCompBarcodeScanning };
function createAllCSS() {
    return [
        css `
      position: fixed;
      left: 0px;
      top: 0px;
      width: 0px;
      height: 0px;
      overflow: hidden;
    `
    ];
}
