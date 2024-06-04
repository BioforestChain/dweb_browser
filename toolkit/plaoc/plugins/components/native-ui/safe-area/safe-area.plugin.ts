import { bindThis } from "../../../helper/bindThis.ts";
import { BasePlugin } from "../../base/base.plugin.ts";
// import { $SafeAreaWritableState } from "./safe-area.type.ts";
export class SafeAreaPlugin extends BasePlugin {
  constructor() {
    super("window.sys.dweb");
  }
  // @bindThis
  // async setState(state: Partial<$SafeAreaWritableState>) {
  // }
  // @bindThis
  // setStateByKey<K extends keyof $SafeAreaWritableState>(key: K, value: $SafeAreaWritableState[K]) {
  //   return this.setState({ [key]: value });
  // }
  // @bindThis
  //  setOverlay(overlay: boolean) {
  // }
  @bindThis
  getOverlay() {
    return false;
  }
}

export const safeAreaPlugin = new SafeAreaPlugin();
