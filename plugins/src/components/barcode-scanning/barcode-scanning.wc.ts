import { barcodeScannerPlugin } from "./barcode-scanning.plugin.ts";

export class HTMLDwebBarcodeScanningElement extends HTMLElement {
  plugin = barcodeScannerPlugin;
}

customElements.define(barcodeScannerPlugin.tagName, HTMLDwebBarcodeScanningElement);
