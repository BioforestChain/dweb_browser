import { bluetoothPlugin } from "./bluetooth.plugin.ts";
export class HTMLBluetoothElement extends HTMLElement {
  static readonly tagName = "dweb-bluetooth";
  plugin = bluetoothPlugin;

  async open() {
    return await this.plugin.open();
  }

  async close() {
    return await this.plugin.close();
  }

  async requestAndConnectDevice(options?: RequestDeviceOptions) {
    return await this.plugin.requestAndConnectDevice(options);
  }
}

// 注册
if (!customElements.get(HTMLBluetoothElement.tagName)) {
  customElements.define(HTMLBluetoothElement.tagName, HTMLBluetoothElement);
}

declare global {
  interface HTMLElementTagNameMap {
    [HTMLBluetoothElement.tagName]: HTMLBluetoothElement;
  }
}
