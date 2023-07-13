import { BasePlugin } from "../components/base/BasePlugin.ts";
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

  // async *jsonlines(options?: { signal?: AbortSignal }) {
  //   const jsonlines = await this.plugin
  //     .buildInternalApiRequest("/observe", {
  //       search: { mmid: this.plugin.mmid },
  //       base: await BasePlugin.public_url,
  //     })
  //     .fetch()
  //     .jsonlines(this.coder.decode);
  //   for await (const state of streamRead(jsonlines, options)) {
  //     this.currentState = state;
  //     yield state;
  //   }
  // }

  async *jsonlines(options?: { signal?: AbortSignal }) {
    let controller: ReadableStreamDefaultController;
    const readableStream: ReadableStream = new ReadableStream({
      start(_controller) {
        controller = _controller;
      },
      pull() {},
      cancel() {},
    });

    const url = new URL(BasePlugin.url.replace(/^http:/, "ws:").replace(/^https:/, "wss:"));
    url.pathname = `${this.plugin.mmid}/observe`;
    const ws = new WebSocket(url);
    ws.onerror = (err) => {
      console.error("onerror", err);
      controller.close();
    };
    // let count = 0;
    ws.onopen = () => {};
    ws.onmessage = async (event: MessageEvent<Blob>) => {
      const str = await event.data.text();
      if (str.length === 0) return;
      console.log("str: ", str);
      const value = this.coder.decode(JSON.parse(str));
      controller.enqueue(value);
    };

    ws.onclose = () => {
      controller.close();
      console.log("关闭了 ws");
    };

    for await (const state of streamRead(readableStream, options)) {
      this.currentState = state;
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
