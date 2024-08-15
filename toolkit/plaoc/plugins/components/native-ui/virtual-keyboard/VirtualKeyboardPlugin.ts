import { cacheGetter } from "../../../../../dweb-helper/src/cacheGetter.ts";
import { bindThis } from "../../../helper/bindThis.ts";
import { DOMInsets, domInsetsToJson, insetsToDom } from "../../../util/insets.ts";
import { StateObserver, type $Coder } from "../../../util/StateObserver.ts";
import { BasePlugin } from "../../base/base.plugin.ts";
import { windowPlugin } from "../../window/window.plugin.ts";
import type {
  $VirtualKeyboardRawState,
  $VirtualKeyboardState,
  $VirtualKeyboardWritableState,
} from "./virtual-keyboard.type.ts";

export class VirtualKeyboardPlugin extends BasePlugin {
  constructor() {
    super("window.sys.dweb");
  }

  coder: $Coder<$VirtualKeyboardRawState, $VirtualKeyboardState> = {
    decode: (raw) => ({
      overlay: raw.overlay,
      insets: insetsToDom(raw.insets),
    }),
    encode: (state) => ({
      overlay: state.overlay,
      insets: domInsetsToJson(state.insets),
      // ...state,
    }),
  };
  @cacheGetter()
  get state() {
    return new StateObserver(
      this,
      () => this.getState(),
      this.coder,
      async (url) => {
        url.pathname = "/observe-keyboard";
        url.searchParams.set("wid", (await windowPlugin.windowInfo).wid);
      }
    );
  }

  @bindThis
  async setState(state: Partial<$VirtualKeyboardWritableState>) {
    await windowPlugin.setStyle({
      keyboardOverlaysContent: state.overlay,
    });
  }
  @bindThis
  setStateByKey<K extends keyof $VirtualKeyboardWritableState>(key: K, value: $VirtualKeyboardWritableState[K]) {
    return this.setState({ [key]: value });
  }
  @bindThis
  async getState() {
    const winState = await windowPlugin.getState();
    return {
      overlay: winState.keyboardOverlaysContent,
      insets: new DOMInsets(0, 0, 0, 0),
    };
  }
  @bindThis
  setOverlay(overlay: boolean) {
    return this.setStateByKey("overlay", overlay);
  }
  @bindThis
  async getOverlay() {
    return (await this.getState()).overlay;
  }
}
