import { webviewPlugin } from "./webview.plugin.ts";

export class HTMLWebviewElement extends HTMLElement {
  static readonly tagName = "dweb-webview";
  plugin = webviewPlugin;

  open(url: string) {
    return this.plugin.open(url);
  }

  close(webviewId: string) {
    return this.plugin.close(webviewId);
  }

  activate() {
    return this.plugin.activate();
  }

  closeWindow() {
    return this.plugin.closeApp();
  }
}

customElements.define(HTMLWebviewElement.tagName, HTMLWebviewElement);
declare global {
  interface HTMLElementTagNameMap {
    [HTMLWebviewElement.tagName]: HTMLWebviewElement;
  }
}