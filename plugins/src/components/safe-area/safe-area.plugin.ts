import { bindThis } from "../../helper/bindThis.ts";
import { cacheGetter } from "../../helper/cacheGetter.ts";
import { domInsetsToJson, insetsToDom } from "../../util/insets.ts";
import { $Coder, StateObserver } from "../../util/StateObserver.ts";
import { BasePlugin } from "../basePlugin.ts";
import {
  $SafeAreaRawState,
  $SafeAreaState,
  $SafeAreaWritableState,
} from "./safe-area.type.ts";

export class SafeAreaPlugin extends BasePlugin {
  readonly tagName = "dweb-safe-area";
  constructor() {
    super("safe-area.native-ui.sys.dweb");
  }

  readonly coder: $Coder<$SafeAreaRawState, $SafeAreaState> = {
    decode: (value) => ({
      ...value,
      cutoutInsets: insetsToDom(value.cutoutInsets),
      outerInsets: insetsToDom(value.outerInsets),
      innerInsets: insetsToDom(value.innerInsets),
    }),
    encode: (value) => ({
      ...value,
      cutoutInsets: domInsetsToJson(value.cutoutInsets),
      outerInsets: domInsetsToJson(value.outerInsets),
      innerInsets: domInsetsToJson(value.innerInsets),
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
  @bindThis
  async setStates(state: Partial<$SafeAreaWritableState>) {
    await this.fetchApi("/setState", {
      search: state,
    });
  }
  @bindThis
  setState<K extends keyof $SafeAreaWritableState>(
    key: K,
    value: $SafeAreaWritableState[K]
  ) {
    return this.setStates({
      [key]: value,
    });
  }
  get getState() {
    return this.state.getState;
  }
  @bindThis
  setOverlay(overlay: boolean) {
    return this.setState("overlay", overlay);
  }
  @bindThis
  async getOverlay() {
    return (await this.state.getState()).overlay;
  }
}

export const safeAreaPlugin = new SafeAreaPlugin();
