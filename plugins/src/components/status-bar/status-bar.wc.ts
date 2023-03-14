import { streamRead } from "../../helper/readableStreamHelper.ts";
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
  constructor() {
    super();
    (async () => {
      for await (const info of streamRead(await statusBarPlugin.observe())) {
        console.log("changed", info);
        statusBarPlugin.currentInfo = info;
      }
    })();
  }
  async connectedCallback() {
    await statusBarPlugin.startObserve();
    await statusBarPlugin.getInfo();
  }
  async disconnectedCallback() {
    await statusBarPlugin.stopObserve();
  }
}

customElements.define(statusBarPlugin.tagName, HTMLDwebStatusBarElement);
