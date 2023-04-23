/// <reference lib="dom" />
import { networkPlugin } from "./network.plugin.ts";
import { cacheGetter } from "../../helper/cacheGetter.ts";

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
