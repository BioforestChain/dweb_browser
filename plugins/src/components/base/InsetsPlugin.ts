import { bindThis } from "../../helper/bindThis.ts";
import { cacheGetter } from "../../helper/cacheGetter.ts";
import {
  $Insets,
  DOMInsets,
  domInsetsToJson,
  insetsToDom,
} from "../../util/insets.ts";
import { $Coder, StateObserver } from "../../util/StateObserver.ts";
import { BasePlugin } from "./BasePlugin.ts";

export interface $InsetsRawState {
  overlay: boolean;
  insets: $Insets;
}
export interface $InsetsState {
  overlay: boolean;
  insets: DOMInsets;
}
export interface $InsetsWritableState {
  overlay: boolean;
}

export abstract class InsetsPlugin<
  RAW extends $InsetsRawState,
  STATE extends $InsetsState,
  WRITABLE_STATE extends $InsetsWritableState
> extends BasePlugin {
  abstract coder: $Coder<RAW, STATE>;
  protected baseCoder = {
    decode: <RAW extends $InsetsRawState>(raw: RAW) => ({
      ...raw,
      insets: insetsToDom(raw.insets),
    }),
    encode: <STATE extends $InsetsState>(state: STATE) => ({
      ...state,
      insets: domInsetsToJson(state.insets),
    }),
  };

  @cacheGetter()
  get state() {
    return new StateObserver(
      this,
      () => this.commonGetState<RAW>(),
      this.coder
    );
  }

  abstract setState(state: Partial<WRITABLE_STATE>): Promise<unknown>;
  abstract setStateByKey<K extends keyof WRITABLE_STATE>(
    key: K,
    value: WRITABLE_STATE[K]
  ): Promise<unknown>;

  @bindThis
  protected async commonSetState<WS extends $InsetsWritableState>(
    state: Partial<WS>
  ) {
    await this.fetchApi("/setState", {
      search: state,
    }).ok();
  }
  static rid_acc = 1;
  @bindThis
  protected async commonGetState<S extends $InsetsRawState>() {
    return await this.fetchApi("/getState", {
      search: { rid: InsetsPlugin.rid_acc++ },
    }).object<S>();
  }

  get getState() {
    return this.state.getState;
  }
  @bindThis
  setOverlay(overlay: boolean) {
    return this.setStateByKey("overlay", overlay);
  }
  @bindThis
  async getOverlay() {
    return (await this.state.getState()).overlay;
  }
}
