import { BasePlugin } from "../components/base/base.plugin.ts";
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

  async *jsonlines(path: string, options?: { signal?: AbortSignal, searchParams?: URLSearchParams }) {
    const api_url = BasePlugin.api_url;
    const url = new URL(api_url.replace(/^http/, "ws"));
    // 内部的监听
    url.pathname = `/${this.plugin.mmid}${path}`;
    options?.searchParams?.forEach((v, k) => {
      url.searchParams.append(k, v);
    });
    
    const ws = new WebSocket((await this.buildWsUrl(url)) ?? url);
    this._ws = ws;
    ws.binaryType = "arraybuffer";
    const streamout = new ReadableStreamOut<string>();

    ws.onmessage = (event) => {
      const data = event.data as string;
      streamout.controller.enqueue(data);
    };
    ws.onclose = () => {
      streamout.controller?.close();
    };
    ws.onerror = (event) => {
      streamout.controller.error(event);
    };

    for await (const state of streamRead(
      streamout.stream.pipeThrough(new JsonlinesStream(this.coder.decode)),
      options
    )) {
      yield state;
    }
  }

  close() {
    this._ws?.close();
  }
}
