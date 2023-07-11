import { devicePlugin } from "./device.plugin.ts";

export class HTMLDeviceElement extends HTMLElement {
  static readonly tagName = "dweb-device";
  plugin = devicePlugin;

  async getUUID() {
    return this.plugin.getUUID();
  }
}

// 注册
customElements.define(HTMLDeviceElement.tagName, HTMLDeviceElement);
