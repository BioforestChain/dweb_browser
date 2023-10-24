import { cacheGetter } from "../../helper/cacheGetter.ts";
import { HTMLStateObserverElement } from "../../util/HTMLStateObserverElement.ts";
import { virtualKeyboardPlugin } from "./virtual-keyboard.plugin.ts";
import { $VirtualKeyboardRawState, $VirtualKeyboardState } from "./virtual-keyboard.type.ts";

export class HTMLDwebVirtualKeyboardElement extends HTMLStateObserverElement<
  $VirtualKeyboardRawState,
  $VirtualKeyboardState
> {
  static readonly tagName = "dweb-virtual-keyboard";
  readonly plugin = virtualKeyboardPlugin;
  constructor() {
    super(virtualKeyboardPlugin.state);
  }
  @cacheGetter()
  get getState() {
    return virtualKeyboardPlugin.getState;
  }
  @cacheGetter()
  get setState() {
    return virtualKeyboardPlugin.setStateByKey;
  }
  @cacheGetter()
  get setOverlay() {
    return virtualKeyboardPlugin.setOverlay;
  }
  @cacheGetter()
  get getOverlay() {
    return virtualKeyboardPlugin.getOverlay;
  }
}

if (!customElements.get(HTMLDwebVirtualKeyboardElement.tagName)) {
  customElements.define(HTMLDwebVirtualKeyboardElement.tagName, HTMLDwebVirtualKeyboardElement);
}
declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebVirtualKeyboardElement.tagName]: HTMLDwebVirtualKeyboardElement;
  }
}
