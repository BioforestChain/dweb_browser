import { geolocationPlugin } from "./geolocation.plugin.ts";

export class HTMLGeolocationElement extends HTMLElement {
  static readonly tagName = "dweb-geolocation";
  plugin = geolocationPlugin;

  getLocation() {
    return this.plugin.getLocation();
  }
}

if (!customElements.get(HTMLGeolocationElement.tagName)) {
  customElements.define(HTMLGeolocationElement.tagName, HTMLGeolocationElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLGeolocationElement.tagName]: HTMLGeolocationElement;
  }
}
