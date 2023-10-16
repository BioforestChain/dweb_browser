import { cacheGetter } from "../../helper/cacheGetter.ts";
import { statusBarPlugin } from "./status-bar.plugin.ts";

export class HTMLDwebStatusBarElement extends HTMLElement {
  static readonly tagName = "dweb-status-bar";
  readonly plugin = statusBarPlugin;
  @cacheGetter()
  get setColor() {
    return statusBarPlugin.setColor;
  }
  @cacheGetter()
  get getColor() {
    return statusBarPlugin.getColor;
  }
  @cacheGetter()
  get setStyle() {
    return statusBarPlugin.setStyle;
  }
  @cacheGetter()
  get getStyle() {
    return statusBarPlugin.getStyle;
  }
  @cacheGetter()
  get show() {
    return statusBarPlugin.show;
  }
  @cacheGetter()
  get hide() {
    return statusBarPlugin.hide;
  }
  @cacheGetter()
  get setVisible() {
    return statusBarPlugin.setVisible;
  }
  @cacheGetter()
  get getVisible() {
    return statusBarPlugin.getVisible;
  }
  @cacheGetter()
  get getState() {
    return statusBarPlugin.getState;
  }
  @cacheGetter()
  get setOverlay() {
    return statusBarPlugin.setOverlay;
  }
  @cacheGetter()
  get getOverlay() {
    return statusBarPlugin.getOverlay;
  }
}

customElements.define(HTMLDwebStatusBarElement.tagName, HTMLDwebStatusBarElement);

declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebStatusBarElement.tagName]: HTMLDwebStatusBarElement;
  }
}
