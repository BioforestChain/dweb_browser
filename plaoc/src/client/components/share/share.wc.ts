import { sharePlugin } from "./share.plugin.ts";

export class HTMLDwebShareElement extends HTMLElement {
  static readonly tagName = "dweb-share";
  readonly plugin = sharePlugin;

  get canShare() {
    return sharePlugin.canShare;
  }
  get share() {
    return sharePlugin.share;
  }
}

customElements.define(HTMLDwebShareElement.tagName, HTMLDwebShareElement);
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebShareElement.tagName]: HTMLDwebShareElement;
  }
}
