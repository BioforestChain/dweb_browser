import { cacheGetter } from "../../helper/cacheGetter.ts";
import { HTMLStateObserverElement } from "../../util/HTMLStateObserverElement.ts";
import { safeAreaPlugin } from "./safe-area.plugin.ts";
import { $SafeAreaState } from "./safe-area.type.ts";
import { $SafeAreaRawState } from "./safe-area.type.ts";

export class HTMLDwebSafeAreaElement extends HTMLStateObserverElement<
  $SafeAreaRawState,
  $SafeAreaState
> {
  constructor() {
    super(safeAreaPlugin.state);
  }
  @cacheGetter()
  get getState() {
    return safeAreaPlugin.state.getState;
  }
}
customElements.define(safeAreaPlugin.tagName, HTMLDwebSafeAreaElement);
