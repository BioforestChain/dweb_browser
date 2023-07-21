import { BasePlugin } from "../components/base/BasePlugin.ts";
import { $Transform, JsonlinesStream } from "../helper/JsonlinesStream.ts";
import { bindThis } from "../helper/bindThis.ts";
import { $Callback, createSignal } from "../helper/createSignal.ts";
import { streamRead } from "../helper/readableStreamHelper.ts";
import { PromiseOut } from './../helper/PromiseOut.ts';

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
    const readableStream: PromiseOut<ReadableStream<STATE>> = new PromiseOut();
 
    const pub_url = await BasePlugin.public_url;
    const url = new URL(pub_url.replace(/^http:/, "ws:"));
    // 内部的监听
    url.pathname = `/observe`;
    url.searchParams.append("mmid",this.plugin.mmid)
    const ws = new WebSocket(url);
    this._ws = ws;
    ws.onerror = async () => {
      (await readableStream.promise).cancel()
    };
    ws.onmessage = async (event) => {
      try {
      const data = event.data;
       if (data instanceof Blob) {
       const stream = data.stream()
        //先转换为utf8 
        .pipeThrough(new TextDecoderStream())
        // 然后交给 jsonlinesStream 来处理
        .pipeThrough(new JsonlinesStream(this.coder.decode))
        readableStream.resolve(stream)
        } else {
          throw new Error("should not happend");
        }
      } catch (err) {
        console.error(err);
      }
    };

    ws.onclose = async () => {
      (await readableStream.promise).cancel()
    };
    const read = await readableStream.promise;

    for await (const state of streamRead(read, options)) {
      console.log("🥰 get state=>",state)
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
