import { statusBarPlugin } from "./status-bar.plugin.ts";

export class HTMLDwebStatusBarElement extends HTMLElement {
  readonly plugin = statusBarPlugin;
  get setBackgroundColor() {
    return statusBarPlugin.setBackgroundColor;
  }
  get getBackgroundColor() {
    return statusBarPlugin.getBackgroundColor;
  }
  get setStyle() {
    return statusBarPlugin.setStyle;
  }
  get getStyle() {
    return statusBarPlugin.getStyle;
  }
  get show() {
    return statusBarPlugin.show;
  }
  get hide() {
    return statusBarPlugin.hide;
  }
  get getInfo() {
    return statusBarPlugin.getInfo;
  }
  get setOverlaysWebView() {
    return statusBarPlugin.setOverlaysWebView;
  }
  get getOverlaysWebView() {
    return statusBarPlugin.getOverlaysWebView;
  }
}

customElements.define(statusBarPlugin.tagName, HTMLDwebStatusBarElement);
