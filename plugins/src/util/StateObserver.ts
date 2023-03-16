import { BasePlugin } from "../components/basePlugin.ts";
import { bindThis } from "../helper/bindThis.ts";
import { $Callback, createSignal } from "../helper/createSignal.ts";
import { $Transform } from "../helper/JsonlinesStream.ts";
import { streamRead } from "../helper/readableStreamHelper.ts";

/**
 * 提供了一个状态的读取与更新的功能
 */
export class StateObserver<RAW, STATE> {
  constructor(
    private plugin: BasePlugin,
    private fetcher: () => Promise<RAW>,
    private coder: {
      decode: $Transform<RAW, STATE>;
      encode: $Transform<STATE, RAW>;
    }
  ) {}
  startObserve() {
    return this.plugin.fetchApi(`/startObserve`);
  }

  async *jsonlines(options?: { signal?: AbortSignal }) {
    const jsonlines = await this.plugin
      .buildInternalApiRequest("/observe", {
        search: { mmid: this.plugin.mmid },
        base: await BasePlugin.public_url,
      })
      .fetch()
      .jsonlines(this.coder.encode);
    for await (const state of streamRead(jsonlines, options)) {
      this.currentState = this.coder.decode(state);
      yield state;
    }
  }

  stopObserve() {
    return this.plugin.fetchApi(`/stopObserve`);
  }

  private _currentState?: STATE;
  /**
   * 当前的状态集合
   */
  public get currentState() {
    return this._currentState;
  }
  public set currentState(state) {
    this._currentState = state;
    if (state) {
      this._signalCurrentStateChange.emit(state);
    }
  }
  private _signalCurrentStateChange = createSignal<$Callback<[STATE]>>();
  readonly onChange = this._signalCurrentStateChange.listen;

  /**
   * 获取当前状态栏的完整信息
   * @returns
   */
  @bindThis
  async getState(force_update = false) {
    if (force_update || this.currentState === undefined) {
      return await this._updateCurrentState();
    }
    return this.currentState;
  }
  /**
   * 刷新获取有关状态栏当前状态的信息。
   */
  private async _updateCurrentState() {
    const raw = await this.fetcher();
    return (this.currentState = this.coder.decode(raw));
  }
}

export interface $Coder<RAW, STATE> {
  decode: $Transform<RAW, STATE>;
  encode: $Transform<STATE, RAW>;
}