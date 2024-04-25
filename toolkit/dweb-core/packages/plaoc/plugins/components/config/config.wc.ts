import { cacheGetter } from "../../helper/cacheGetter.ts";
import { configPlugin } from "./config.plugin.ts";

export class HTMLDwebConfigElement extends HTMLElement {
  static readonly tagName = "dweb-config";
  readonly plugin = configPlugin;

  @cacheGetter()
  get setLang() {
    return this.plugin.setLang;
  }

  @cacheGetter()
  get getLang() {
    return this.plugin.getLang;
  }
}
if (!customElements.get(HTMLDwebConfigElement.tagName)) {
  customElements.define(HTMLDwebConfigElement.tagName, HTMLDwebConfigElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebConfigElement.tagName]: HTMLDwebConfigElement;
  }
}
