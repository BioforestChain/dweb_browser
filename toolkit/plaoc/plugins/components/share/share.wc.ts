import { cacheGetter } from "../../helper/cacheGetter.ts";
import { sharePlugin } from "./share.plugin.ts";

export class HTMLDwebShareElement extends HTMLElement {
  static readonly tagName = "dweb-share";
  readonly plugin = sharePlugin;

  @cacheGetter()
  get canShare() {
    return sharePlugin.canShare;
  }

  @cacheGetter()
  get share() {
    return sharePlugin.share;
  }
}

if (!customElements.get(HTMLDwebShareElement.tagName)) {
  customElements.define(HTMLDwebShareElement.tagName, HTMLDwebShareElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebShareElement.tagName]: HTMLDwebShareElement;
  }
}
