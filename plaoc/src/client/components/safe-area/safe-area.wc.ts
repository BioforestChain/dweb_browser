import { cacheGetter } from "../../helper/cacheGetter.ts";
import { safeAreaPlugin } from "./safe-area.plugin.ts";

export class HTMLDwebSafeAreaElement extends HTMLElement {
  static readonly tagName = "dweb-safe-area";
  readonly plugin = safeAreaPlugin;


  @cacheGetter()
  get setState() {
    return safeAreaPlugin.setStateByKey;
  }
  @cacheGetter()
  get setOverlay() {
    return safeAreaPlugin.setOverlay;
  }
  @cacheGetter()
  get getOverlay() {
    return safeAreaPlugin.getOverlay;
  }
}
customElements.define(HTMLDwebSafeAreaElement.tagName, HTMLDwebSafeAreaElement);

declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebSafeAreaElement.tagName]: HTMLDwebSafeAreaElement;
  }
}
