import { cacheGetter } from "@dweb-browser/helper/cacheGetter.ts";
import { navigationBarPlugin } from "./navigation-bar.plugin.ts";

export class HTMLDwebNavigationBarElement extends HTMLElement {
  static readonly tagName = "dweb-navigation-bar";
  readonly plugin = navigationBarPlugin;
  @cacheGetter()
  get setColor() {
    return navigationBarPlugin.setColor;
  }
  @cacheGetter()
  get getColor() {
    return navigationBarPlugin.getColor;
  }
  @cacheGetter()
  get setStyle() {
    return navigationBarPlugin.setStyle;
  }
  @cacheGetter()
  get getStyle() {
    return navigationBarPlugin.getStyle;
  }
  @cacheGetter()
  get setVisible() {
    return navigationBarPlugin.setVisible;
  }
  @cacheGetter()
  get getVisible() {
    return navigationBarPlugin.getVisible;
  }
  @cacheGetter()
  get getState() {
    return navigationBarPlugin.getState;
  }
  @cacheGetter()
  get setOverlay() {
    return navigationBarPlugin.setOverlay;
  }
  @cacheGetter()
  get getOverlay() {
    return navigationBarPlugin.getOverlay;
  }
}

if (!customElements.get(HTMLDwebNavigationBarElement.tagName)) {
  customElements.define(HTMLDwebNavigationBarElement.tagName, HTMLDwebNavigationBarElement);
}

declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebNavigationBarElement.tagName]: HTMLDwebNavigationBarElement;
  }
}
