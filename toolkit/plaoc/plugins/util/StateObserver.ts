import type { Ipc } from "@dweb-browser/core/ipc/ipc.ts";
import { createSignal, type $Callback } from "@dweb-browser/helper/createSignal.ts";
import { BasePlugin } from "../components/base/base.plugin.ts";

import { jsonlinesStreamReadText } from "@dweb-browser/helper/stream/jsonlinesStreamHelper.ts";
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
  private ws?: WebSocket;

  /**监听状态变化 */
  async jsonlines(options?: { signal?: AbortSignal }) {
    const api_url = BasePlugin.api_url;
    const url = new URL(api_url.replace(/^http/, "ws"));
    // 内部的监听
    url.pathname = `/${this.plugin.mmid}/observe`;
    const wsUrl = ((await this.buildWsUrl(url)) ?? url).href;
    return jsonlinesStreamReadText<STATE>(
      new ReadableStream<string>({
        pull: (controller) => {
          this.pullMessage(wsUrl, controller);
        },
        cancel: () => {
          this.ws?.close();
        },
      }),
      options
    );
  }
  // 断开重连机制
  pullMessage(wsUrl: string, controller: ReadableStreamDefaultController<string>) {
    if (this.ws == null || this.ws.readyState === WebSocket.CLOSING) {
      console.log(`re-create=>${wsUrl}`, this.ws?.readyState);
      this.ws = new WebSocket(wsUrl);
      this.ws.onmessage = (msgEvent) => {
        const data = msgEvent.data;
        // console.log("ws on message", data, typeof data, wsUrl);
        if (typeof data === "string") {
          this.currentState = JSON.parse(data);
          // console.log("pull", this.currentState);
          controller.enqueue(data);
        }
      };
    }
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
