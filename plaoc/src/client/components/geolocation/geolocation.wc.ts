import { cacheGetter } from "../../helper/cacheGetter.ts";
import { geolocationPlugin } from "./geolocation.plugin.ts";
import { $LocationOptions } from "./geolocation.type.ts";

export class HTMLGeolocationElement extends HTMLElement {
  static readonly tagName = "dweb-geolocation";
  plugin = geolocationPlugin;

  @cacheGetter()
  get getLocation() {
    return this.plugin.getLocation;
  }
 
  async createLocation(option?: $LocationOptions) {
    const controller = await this.plugin.createLocation(option);
    controller.listen((location) => {
      this.dispatchEvent(new CustomEvent("location", { detail: location }));
    });
    return controller;
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
