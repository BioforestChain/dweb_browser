import { $OffListener } from "../../helper/createSignal.ts";
import { statusBarPlugin } from "./status-bar.plugin.ts";

export class HTMLDwebStatusBarElement extends HTMLElement {
  readonly plugin = statusBarPlugin;
  get setColor() {
    return statusBarPlugin.setColor;
  }
  get getColor() {
    return statusBarPlugin.getColor;
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
  get getState() {
    return statusBarPlugin.state.getState();
  }
  get setOverlay() {
    return statusBarPlugin.setOverlay;
  }
  get getOverlay() {
    return statusBarPlugin.getOverlay;
  }
  constructor() {
    super();
    (async () => {
      for await (const state of statusBarPlugin.state.jsonlines()) {
        console.log("status-bar changed", state);
      }
    })();
  }
  private _onchange?: $OffListener;
  async connectedCallback() {
    this._onchange = statusBarPlugin.state.onChange((info) => {
      this.dispatchEvent(new CustomEvent("change", { detail: info }));
    });
    await statusBarPlugin.state.startObserve(); // 开始监听
    await statusBarPlugin.getState(true); // 强制刷新
  }
  async disconnectedCallback() {
    if (this._onchange) {
      this._onchange();
      this._onchange = undefined;
    }
    await statusBarPlugin.state.stopObserve();
  }
}

customElements.define(statusBarPlugin.tagName, HTMLDwebStatusBarElement);
