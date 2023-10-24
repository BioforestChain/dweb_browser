import { cacheGetter } from "../../helper/cacheGetter.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
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

  get public_url() {
    return this.plugin.public_url;
  }
  getPublicUrl() {
    return BasePlugin.public_url;
  }
  static get observedAttributes() {
    return ["api-url"];
  }
  attributeChangedCallback(name: string, _oldValue: string, newValue: string) {
    if (name === "api-url") {
      configPlugin.setInternalUrl(newValue);
    }
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
