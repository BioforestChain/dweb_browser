import { configPlugin } from "./config.plugin.ts";

export class HTMLDwebConfigElement extends HTMLElement {
  readonly plugin = configPlugin;
  get public_url() {
    return configPlugin.public_url;
  }

  getPublicUrl() {
    return configPlugin.getPublicUrl();
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

customElements.define(configPlugin.tagName, HTMLDwebConfigElement);
