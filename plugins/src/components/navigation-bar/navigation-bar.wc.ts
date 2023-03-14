import { navigatorBarPlugin } from "./navigation-bar.plugin.ts";

export class HTMLDwebNavigationBarElement extends HTMLElement {
  readonly plugin = navigatorBarPlugin;
  get show() {
    return navigatorBarPlugin.show;
  }
  get hide() {
    return navigatorBarPlugin.hide;
  }
  get getVisible() {
    return navigatorBarPlugin.getVisible;
  }
  get setColor() {
    return navigatorBarPlugin.setColor;
  }
  get getColor() {
    return navigatorBarPlugin.getColor;
  }
  get setTransparency() {
    return navigatorBarPlugin.setTransparency;
  }
  get getTransparency() {
    return navigatorBarPlugin.getTransparency;
  }
  get setOverlay() {
    return navigatorBarPlugin.setOverlay;
  }
  get getOverlay() {
    return navigatorBarPlugin.getOverlay;
  }
}

customElements.define(navigatorBarPlugin.tagName, HTMLDwebNavigationBarElement);
