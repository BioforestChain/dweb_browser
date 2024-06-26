import { cacheGetter } from "@dweb-browser/helper/cacheGetter.ts";
import { biometricsPlugin } from "./biometrics.plugin.ts";

export class HTMLDwebBiometricsElement extends HTMLElement {
  static readonly tagName = "dweb-biometrics";
  readonly plugin = biometricsPlugin;

  @cacheGetter()
  get check() {
    return this.plugin.check;
  }

  @cacheGetter()
  get biometrics() {
    return this.plugin.biometrics;
  }
}

if (!customElements.get(HTMLDwebBiometricsElement.tagName)) {
  customElements.define(HTMLDwebBiometricsElement.tagName, HTMLDwebBiometricsElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebBiometricsElement.tagName]: HTMLDwebBiometricsElement;
  }
}
