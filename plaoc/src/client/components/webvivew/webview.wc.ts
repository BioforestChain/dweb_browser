import { webviewPlugin } from "./webview.plugin.ts";

export class HTMLWebviewElement extends HTMLElement {
  static readonly tagName = "dweb-webview";
  plugin = webviewPlugin;

  async open(url: string) {
    return await this.plugin.open(url);
  }

  async close(host: string) {
    return await this.plugin.close(host);
  }

  async activate() {
    return await this.plugin.activate();
  }

  async closeWindow() {
    return await this.plugin.closeWindow();
  }
}

customElements.define(HTMLWebviewElement.tagName, HTMLWebviewElement);
