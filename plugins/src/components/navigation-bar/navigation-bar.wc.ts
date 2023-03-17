import { cacheGetter } from "../../helper/cacheGetter.ts";
import { $OffListener } from "../../helper/createSignal.ts";
import { navigationBarPlugin } from "./navigation-bar.plugin.ts";

export class HTMLDwebNavigationBarElement extends HTMLElement {
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
  get show() {
    return navigationBarPlugin.show;
  }
  @cacheGetter()
  get hide() {
    return navigationBarPlugin.hide;
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

customElements.define(
  navigationBarPlugin.tagName,
  HTMLDwebNavigationBarElement
);
