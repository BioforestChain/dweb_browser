import { $OffListener } from "../../helper/createSignal.ts";
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
  get setVisible() {
    return statusBarPlugin.setVisible;
  }
  get getVisible() {
    return statusBarPlugin.getVisible;
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
        statusBarPlugin.currentInfo = statusBarPlugin.normalizeRawInfo(info);
      }
    })();
  }
  private _onchange?: $OffListener;
  async connectedCallback() {
    this._onchange = statusBarPlugin.onCurrentInfoChange((info) => {
      this.dispatchEvent(new CustomEvent("change", { detail: info }));
    });
    await statusBarPlugin.startObserve(); // 开始监听
    await statusBarPlugin.getInfo(true); // 强制刷新
  }
  async disconnectedCallback() {
    if (this._onchange) {
      this._onchange();
      this._onchange = undefined;
    }
    await statusBarPlugin.stopObserve();
  }
}

customElements.define(statusBarPlugin.tagName, HTMLDwebStatusBarElement);
