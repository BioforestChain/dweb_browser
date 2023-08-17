import { BasePlugin } from "../base/BasePlugin.ts";
import { configPlugin } from "./config.plugin.ts";

export class HTMLDwebConfigElement extends HTMLElement {
  static readonly tagName = "dweb-config";
  readonly plugin = configPlugin;
  get public_url() {
    return configPlugin.public_url;
  }
  getPublicUrl() {
    return BasePlugin.public_url
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

customElements.define(HTMLDwebConfigElement.tagName, HTMLDwebConfigElement);
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebConfigElement.tagName]: HTMLDwebConfigElement;
  }
}
