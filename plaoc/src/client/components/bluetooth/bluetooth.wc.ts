import { bluetoothPlugin } from "./bluetooth.plugin.ts"
export class HTMLBluetoothElement extends HTMLElement{
  static readonly tagName = "dweb-bluetooth";
  plugin = bluetoothPlugin;

  requestDevice = () => {
    return this.plugin.requestDevice();
  }

  requestDeviceCancel = () => {
    return this.plugin.requestDeviceCancel();
  }
}

// 注册
customElements.define(
  HTMLBluetoothElement.tagName,
  HTMLBluetoothElement
)

