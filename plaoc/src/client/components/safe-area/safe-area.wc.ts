import { cacheGetter } from "../../helper/cacheGetter.ts";
import { HTMLStateObserverElement } from "../../util/HTMLStateObserverElement.ts";
import { safeAreaPlugin } from "./safe-area.plugin.ts";
import { $SafeAreaRawState, $SafeAreaState } from "./safe-area.type.ts";

export class HTMLDwebSafeAreaElement extends HTMLStateObserverElement<
  $SafeAreaRawState,
  $SafeAreaState
> {
  static readonly tagName = "dweb-safe-area";
  readonly plugin = safeAreaPlugin;
  constructor() {
    super(safeAreaPlugin.state);
  }
  @cacheGetter()
  get getState() {
    return safeAreaPlugin.getState;
  }
  @cacheGetter()
  get setState() {
    return safeAreaPlugin.setStateByKey;
  }
  @cacheGetter()
  get setOverlay() {
    return safeAreaPlugin.setOverlay;
  }
  @cacheGetter()
  get getOverlay() {
    return safeAreaPlugin.getOverlay;
  }
}
customElements.define(HTMLDwebSafeAreaElement.tagName, HTMLDwebSafeAreaElement);

declare global {
  interface HTMLElementTagNameMap {
    [HTMLDwebSafeAreaElement.tagName]: HTMLDwebSafeAreaElement;
  }
}
