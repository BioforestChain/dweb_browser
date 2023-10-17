import { BasePlugin } from "../components/base/BasePlugin.ts";
import { $Transform, JsonlinesStream } from "./JsonlinesStream.ts";
import { ReadableStreamOut, streamRead } from "./readableStreamHelper.ts";

export class JsonlinesStreamResponse<RAW, STATE> {
  private _ws: WebSocket | undefined;
  constructor(
    private plugin: BasePlugin,
    private coder: {
      decode: $Transform<RAW, STATE>;
      encode: $Transform<STATE, RAW>;
    },
    private buildWsUrl: (ws_url: URL) => Promise<URL | void> = async (ws_url) => ws_url
  ) {}

  async *jsonlines(pathAndQuery: string, options?: { signal?: AbortSignal }) {
    const pub_url = await BasePlugin.public_url;
    const url = new URL(pub_url.replace(/^http:/, "ws:"));
    // 内部的监听
    url.pathname = `/${this.plugin.mmid}${pathAndQuery}`;
    const ws = new WebSocket((await this.buildWsUrl(url)) ?? url);
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
      yield state;
    }
  }

  close() {
    this._ws?.close();
  }
}
