import { mwebviewPlugin } from "./mwebview.plugin.ts";

export class HTMLMWebviewElement extends HTMLElement {
  static readonly tagName = "dweb-mwebview";
  plugin = mwebviewPlugin;

  open(url: string) {
    return this.plugin.open(url);
  }

  close(webviewId: string) {
    return this.plugin.close(webviewId);
  }

  activate() {
    return this.plugin.activate();
  }

  closeApp() {
    return this.plugin.closeApp();
  }
}
if (!customElements.get(HTMLMWebviewElement.tagName)) {
  customElements.define(HTMLMWebviewElement.tagName, HTMLMWebviewElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLMWebviewElement.tagName]: HTMLMWebviewElement;
  }
}
