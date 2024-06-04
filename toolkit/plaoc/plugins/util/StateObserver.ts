import type { Ipc } from "@dweb-browser/core/ipc/ipc.ts";
import { createSignal, type $Callback } from "@dweb-browser/helper/createSignal.ts";
import { WebSocketIpcBuilder } from "../common/websocketIpc.ts";
import { BasePlugin } from "../components/base/base.plugin.ts";

import { bindThis } from "../helper/bindThis.ts";

type $Transform<T, R> = (value: T) => R;

/**
 * 提供了一个状态的读取与更新的功能
 */
export class StateObserver<RAW, STATE> {
  private _ipc: Ipc | undefined;
  constructor(
    private plugin: BasePlugin,
    private fetcher: () => Promise<RAW>,
    private coder: {
      decode: $Transform<RAW, STATE>;
      encode: $Transform<STATE, RAW>;
    },
    // deno-lint-ignore require-await
    private buildWsUrl: (ws_url: URL) => Promise<URL | void> = async (ws_url) => ws_url
  ) {}
  /**监听状态变化 */
  async startObserveState() {
    const api_url = BasePlugin.api_url;
    const url = new URL(api_url.replace(/^http/, "ws"));
    // 内部的监听
    url.pathname = `/${this.plugin.mmid}/observe`;
    const wsIpcBuilder = new WebSocketIpcBuilder((await this.buildWsUrl(url)) ?? url, this.plugin.self);
    this._ipc = wsIpcBuilder.ipc;
    console.log("connect=>", this._ipc.remote.mmid, url);
    this._ipc.onMessage("state-observer").collect((event) => {
      const data = event.consume();
      console.log(`/${this.plugin.mmid}/observe=>`, data);
      // this.currentState = state;
    });
  }

  stopObserve() {
    this._ipc?.close();
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
