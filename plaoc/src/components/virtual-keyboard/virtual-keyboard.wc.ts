import { cacheGetter } from "../../helper/cacheGetter.ts";
import { HTMLStateObserverElement } from "../../util/HTMLStateObserverElement.ts";
import { virtualKeyboardPlugin } from "./virtual-keyboard.plugin.ts";
import {
  $VirtualKeyboardRawState,
  $VirtualKeyboardState,
} from "./virtual-keyboard.type.ts";

export class HTMLDwebVirtualKeyboardElement extends HTMLStateObserverElement<
  $VirtualKeyboardRawState,
  $VirtualKeyboardState
> {
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
customElements.define(
  virtualKeyboardPlugin.tagName,
  HTMLDwebVirtualKeyboardElement
);
