import { $OffListener } from "../../helper/createSignal.ts";
import { navigationBarPlugin } from "./navigation-bar.plugin.ts";

export class HTMLDwebNavigationBarElement extends HTMLElement {
  readonly plugin = navigationBarPlugin;
  get setColor() {
    return navigationBarPlugin.setColor;
  }
  get getColor() {
    return navigationBarPlugin.getColor;
  }
  get setStyle() {
    return navigationBarPlugin.setStyle;
  }
  get getStyle() {
    return navigationBarPlugin.getStyle;
  }
  get show() {
    return navigationBarPlugin.show;
  }
  get hide() {
    return navigationBarPlugin.hide;
  }
  get setVisible() {
    return navigationBarPlugin.setVisible;
  }
  get getVisible() {
    return navigationBarPlugin.getVisible;
  }
  get getState() {
    return navigationBarPlugin.state.getState();
  }
  get setOverlay() {
    return navigationBarPlugin.setOverlay;
  }
  get getOverlay() {
    return navigationBarPlugin.getOverlay;
  }
  constructor() {
    super();
    (async () => {
      for await (const state of navigationBarPlugin.state.jsonlines()) {
        console.log("navigation-bar changed", state);
      }
    })();
  }
  private _onchange?: $OffListener;
  async connectedCallback() {
    this._onchange = navigationBarPlugin.state.onChange((info) => {
      this.dispatchEvent(new CustomEvent("change", { detail: info }));
    });
    await navigationBarPlugin.state.startObserve(); // 开始监听
    await navigationBarPlugin.getState(true); // 强制刷新
  }
  async disconnectedCallback() {
    if (this._onchange) {
      this._onchange();
      this._onchange = undefined;
    }
    await navigationBarPlugin.state.stopObserve();
  }
}

customElements.define(navigationBarPlugin.tagName, HTMLDwebNavigationBarElement);
