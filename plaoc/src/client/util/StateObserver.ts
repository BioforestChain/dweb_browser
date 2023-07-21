import { BasePlugin } from "../components/base/BasePlugin.ts";
import { $Transform, JsonlinesStream } from "../helper/JsonlinesStream.ts";
import { bindThis } from "../helper/bindThis.ts";
import { $Callback, createSignal } from "../helper/createSignal.ts";
import { ReadableStreamOut, streamRead } from "../helper/readableStreamHelper.ts";

/**
 * 提供了一个状态的读取与更新的功能
 */
export class StateObserver<RAW, STATE> {
  private _ws: WebSocket | undefined;
  constructor(
    private plugin: BasePlugin,
    private fetcher: () => Promise<RAW>,
    private coder: {
      decode: $Transform<RAW, STATE>;
      encode: $Transform<STATE, RAW>;
    }
  ) {}

  async *jsonlines(options?: { signal?: AbortSignal }) {
    const pub_url = await BasePlugin.public_url;
    const url = new URL(pub_url.replace(/^http:/, "ws:"));
    // 内部的监听
    url.pathname = `/${this.plugin.mmid}/observe`;
    const ws = new WebSocket(url);
    this._ws = ws;
    ws.binaryType = "arraybuffer";
    const streamout = new ReadableStreamOut();

    ws.onmessage = async (event) => {
      const data = event.data;
      streamout.controller.enqueue(data);
    };
    ws.onclose = async () => {
      streamout.controller.close();
    };
    ws.onerror = async (event) => {
      streamout.controller.error(event);
    };

    for await (const state of streamRead(
      streamout.stream.pipeThrough(new TextDecoderStream()).pipeThrough(new JsonlinesStream(this.coder.decode)),
      options
    )) {
      this.currentState = state;
      yield state;
    }
  }

  stopObserve() {
    this._ws?.close();
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
