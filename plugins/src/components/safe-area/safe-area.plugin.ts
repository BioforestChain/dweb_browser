import { cacheGetter } from "../../helper/cacheGetter.ts";
import { domRectToJson, rectToDom } from "../../util/rect.ts";
import { $Coder, StateObserver } from "../../util/StateObserver.ts";
import { BasePlugin } from "../basePlugin.ts";
import { $SafeAreaRawState, $SafeAreaState } from "./safe-area.type.ts";

export class SafeAreaPlugin extends BasePlugin {
  readonly tagName = "dweb-safe-area";
  constructor() {
    super("safe-area.native-ui.sys.dweb");
  }

  readonly coder: $Coder<$SafeAreaRawState, $SafeAreaState> = {
    decode: (value) => ({
      ...value,
      cutoutRect: rectToDom(value.cutoutRect),
      boundingOuterRect: rectToDom(value.boundingOuterRect),
      boundingInnerRect: rectToDom(value.boundingInnerRect),
    }),
    encode: (value) => ({
      ...value,
      cutoutRect: domRectToJson(value.cutoutRect),
      boundingOuterRect: domRectToJson(value.boundingOuterRect),
      boundingInnerRect: domRectToJson(value.boundingInnerRect),
    }),
  };

  @cacheGetter()
  get state() {
    return new StateObserver(
      this,
      () => this.fetchApi(`/getState`).object<$SafeAreaRawState>(),
      this.coder
    );
  }
}

export const safeAreaPlugin = new SafeAreaPlugin();
