import { bluetoothPlugin } from "./bluetooth.plugin.ts";
export class HTMLBluetoothElement extends HTMLElement {
  static readonly tagName = "dweb-bluetooth";
  plugin = bluetoothPlugin;

  async open() {
    return this.plugin.open();
  }

  async close() {
    return this.plugin.close();
  }
}

// 注册
customElements.define(HTMLBluetoothElement.tagName, HTMLBluetoothElement);
