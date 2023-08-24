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
  get windowInfo() {
    return this.fetchApi("/window-info", { pathPrefix: "internal" }).object<{ wid: string }>();
  }
  @cacheGetter()
  get state() {
    return new StateObserver(
      this,
      () => this.fetchState<$WindowRawState>(),
      this.coder,
      async (url) => {
        url.searchParams.set("wid", (await this.windowInfo).wid);
      }
    );
  }
  @bindThis
  protected async fetchState<S extends $WindowRawState>() {
    return await this.fetchApi("/getState", { search: await this.windowInfo }).object<S>();
  }
  get getState() {
    return this.state.getState;
  }

  @bindThis
  async setStyle(options: {
    topBarOverlay?: boolean;
    bottomBarOverlay?: boolean;
    topBarContentColor?: string;
    topBarContentDarkColor?: string;
    topBarBackgroundColor?: string;
    topBarBackgroundDarkColor?: string;
    bottomBarContentColor?: string;
    bottomBarContentDarkColor?: string;
    bottomBarBackgroundColor?: string;
    bottomBarBackgroundDarkColor?: string;
    bottomBarTheme?: string;
    themeColor?: string;
    themeDarkColor?: string;
  }) {
    return this.fetchApi("/setStyle", { search: { ...options, ...(await this.windowInfo) } }).void();
  }
}
export const windowPlugin = new WindowPlugin();
