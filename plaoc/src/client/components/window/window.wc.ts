import { cacheGetter } from "../../helper/cacheGetter.ts";
import { HTMLStateObserverElement } from "../../util/HTMLStateObserverElement.ts";
import { windowPlugin } from "./window.plugin.ts";
import { $WindowRawState, $WindowState } from "./window.type.ts";

export class HTMLDwebWindowElement extends HTMLStateObserverElement<$WindowRawState, $WindowState> {
  static readonly tagName = "dweb-window";
  readonly plugin = windowPlugin;
  constructor() {
    super(windowPlugin.state);
  }
  getState = windowPlugin.getState;
  setStyle = windowPlugin.setStyle;

  @cacheGetter()
  get getDisplay() {
    return this.plugin.getDisplay;
  }

  @cacheGetter()
  get focusWindow() {
    return this.plugin.focusWindow;
  }

  @cacheGetter()
  get blurWindow() {
    return this.plugin.blurWindow;
  }

  @cacheGetter()
  get maximize() {
    return this.plugin.maximize;
  }

  @cacheGetter()
  get unMaximize() {
    return this.plugin.unMaximize;
  }

  @cacheGetter()
  get visible() {
    return this.plugin.visible;
  }

  @cacheGetter()
  get close() {
    return this.plugin.close;
  }
}

if (!customElements.get(HTMLDwebWindowElement.tagName)) {
  customElements.define(HTMLDwebWindowElement.tagName, HTMLDwebWindowElement);
}

declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebWindowElement.tagName]: HTMLDwebWindowElement;
  }
}
