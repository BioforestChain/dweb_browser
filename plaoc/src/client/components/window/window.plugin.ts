import { bindThis } from "../../helper/bindThis.ts";
import { cacheGetter } from "../../helper/cacheGetter.ts";
import { $Coder, StateObserver } from "../../util/StateObserver.ts";
import { BasePlugin } from "../base/BasePlugin.ts";
import { $WindowRawState, $WindowState } from "./window.type.ts";
/**
 * 访问 window 能力的插件
 */
export class WindowPlugin extends BasePlugin {
  constructor() {
    super("window.sys.dweb");
  }
  coder: $Coder<$WindowRawState, $WindowState> = {
    decode: (raw) => ({
      ...raw,
    }),
    encode: (state) => ({
      ...state,
    }),
  };
  @cacheGetter()
  get state() {
    return new StateObserver(this, () => this.fetchState<$WindowRawState>(), this.coder);
  }
  @bindThis
  protected async fetchState<S extends $WindowRawState>() {
    return await this.fetchApi("/getState").object<S>();
  }
  get getState() {
    return this.state.getState;
  }

  @bindThis
  setTopBarStyle(options: { contentColor?: string; backgroundColor?: string; overlay?: boolean }) {
    return this.fetchApi("/setTopBarStyle", { search: options }).void();
  }
}
export const windowPlugin = new WindowPlugin();
