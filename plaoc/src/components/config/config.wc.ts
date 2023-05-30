import { configPlugin } from "./config.plugin.ts";

export class HTMLDwebConfigElement extends HTMLElement {
  readonly plugin = configPlugin;
  get public_url() {
    return configPlugin.public_url;
  }

  connectedCallback() {
    const searchParams = new URL(location.href).searchParams;
    const internalUrl = searchParams.get("X-Plaoc-Internal-Url");
    const publicUrl = searchParams.get("X-Plaoc-Public-Url");

    publicUrl && configPlugin.setPublicUrl(publicUrl);
    internalUrl && configPlugin.setInternalUrl(internalUrl);
  }

  getPublicUrl() {
    return configPlugin.updatePublicUrl();
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

// <dweb-config/>
// customElements.define(configPlugin.tagName, HTMLDwebConfigElement);
// <meta is="dweb-config"/>
customElements.define(configPlugin.tagName, HTMLDwebConfigElement, {
  extends: "meta",
});
