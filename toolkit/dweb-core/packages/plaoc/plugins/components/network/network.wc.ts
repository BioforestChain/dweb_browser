import { cacheGetter } from "../../helper/cacheGetter.ts";
import { networkPlugin } from "./network.plugin.ts";

export class HTMLDwebNetworkElement extends HTMLElement {
  static readonly tagName = "dweb-network";
  readonly plugin = networkPlugin;

  @cacheGetter()
  get getStatus() {
    return this.plugin.getStatus;
  }

  @cacheGetter()
  get onLine() {
    return this.plugin.onLine;
  }
}

if (!customElements.get(HTMLDwebNetworkElement.tagName)) {
  customElements.define(HTMLDwebNetworkElement.tagName, HTMLDwebNetworkElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebNetworkElement.tagName]: HTMLDwebNetworkElement;
  }
}
