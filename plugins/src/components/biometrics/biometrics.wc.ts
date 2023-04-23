import { cacheGetter } from "../../helper/cacheGetter.ts";
import { biometricsPlugin } from "./biometrics.plugin.ts";

export class HTMLDwebBiometricsElement extends HTMLElement {

  plugin = biometricsPlugin

  @cacheGetter()
  get chuck() {
    return this.plugin.check
  }

  @cacheGetter()
  get biometrics() {
    return this.plugin.biometrics
  }
}

customElements.define(
  biometricsPlugin.tagName,
  HTMLDwebBiometricsElement
);
