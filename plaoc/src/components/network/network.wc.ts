/// <reference lib="dom" />
import { cacheGetter } from "../../helper/cacheGetter.ts";
import { networkPlugin } from "./network.plugin.ts";

export class HTMLDwebNetworkElement extends HTMLElement {
  plugin = networkPlugin;

  @cacheGetter()
  get getStatus() {
    return this.plugin.getStatus;
  }

  @cacheGetter()
  get onLine() {
    return this.plugin.onLine;
  }
}

customElements.define(networkPlugin.tagName, HTMLDwebNetworkElement);
