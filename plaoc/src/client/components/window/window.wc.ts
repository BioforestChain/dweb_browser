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
  setTopBarStyle = windowPlugin.setTopBarStyle;
}

customElements.define(HTMLDwebWindowElement.tagName, HTMLDwebWindowElement);

declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebWindowElement.tagName]: HTMLDwebWindowElement;
  }
}
