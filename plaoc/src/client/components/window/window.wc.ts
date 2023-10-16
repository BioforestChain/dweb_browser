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
    return this.plugin.getDisplay
  }
}

customElements.define(HTMLDwebWindowElement.tagName, HTMLDwebWindowElement);

declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebWindowElement.tagName]: HTMLDwebWindowElement;
  }
}
