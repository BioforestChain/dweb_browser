import { sharePlugin } from "./share.plugin.ts";

export class HTMLDwebShareElement extends HTMLElement {
  readonly plugin = sharePlugin;

  get canShare() {
    return sharePlugin.canShare;
  }
  get share() {
    return sharePlugin.share;
  }
}

customElements.define(sharePlugin.tagName, HTMLDwebShareElement);
