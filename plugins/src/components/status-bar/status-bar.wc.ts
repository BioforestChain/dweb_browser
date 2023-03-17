import { cacheGetter } from "../../helper/cacheGetter.ts";
import { $OffListener } from "../../helper/createSignal.ts";
import { statusBarPlugin } from "./status-bar.plugin.ts";

export class HTMLDwebStatusBarElement extends HTMLElement {
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
