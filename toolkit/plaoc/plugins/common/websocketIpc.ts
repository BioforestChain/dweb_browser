import { ChannelEndpoint } from "@dweb-browser/core/ipc/endpoint/ChannelEndpoint.ts";
import type { $MicroModuleManifest } from "@dweb-browser/core/types.ts";
import { PromiseOut } from "@dweb-browser/helper/PromiseOut.ts";
import { streamReadAll } from "@dweb-browser/helper/stream/readableStreamHelper.ts";
import { webIpcPool } from "../components/base/base.plugin.ts";

export class WebSocketIpcBuilder {
  #wsUrl;
  #remote;
  #endpoint;
  // 重试次数为5次
  #reAcc = 5;
  readonly ipc;
  constructor(wsUrl: URL, remote: $MicroModuleManifest, reAcc = 5) {
    this.#wsUrl = wsUrl;
    this.#remote = remote;
    this.#reAcc = reAcc;
    this.#endpoint = new ChannelEndpoint(`client-${this.#wsUrl}`);
    this.ipc = webIpcPool.createIpc(this.#endpoint, 0, this.#remote, this.#remote, true);
    /// 响应服务器将响应内容写入serverIpc，这里读取写入的内容，将响应的内容通过代理层传回
    void streamReadAll(this.#endpoint.stream, {
      map: async (chunk) => {
        (await this.getWs()).send(chunk);
      },
    });
  }

  #wspo?: PromiseOut<WebSocket>;
  private async getWs() {
    let wspo = this.#wspo;
    if (wspo?.value?.readyState === WebSocket.CLOSING) {
      wspo = undefined;
    }
    if (wspo === undefined) {
      wspo = new PromiseOut<WebSocket>();
      this.#wspo = wspo;

      while (false === wspo.is_resolved) {
        this.#reAcc--;
        if (this.#reAcc <= 0) {
          console.error("Web Socket has exceeded the maximum number of reconnections!");
          break;
        }
        try {
          const ws = await new Promise<WebSocket>((resolve, reject) => {
            const ws = new WebSocket(this.#wsUrl);
            ws.binaryType = "arraybuffer";

            ws.onclose = () => {
              if (this.#wspo?.value === ws) {
                this.#wspo = undefined;
              }
            };
            ws.onerror = (event) => {
              reject(event);
            };
            ws.onopen = () => {
              resolve(ws);
            };
            /// 客户端发起请求，代理层传递IPC协议内容，响应服务器将这些数据写入proxyStream，然后会进行解析
            ws.onmessage = (event) => {
              try {
                const data = event.data;
                this.#endpoint.send(data);
              } catch (err) {
                console.error("onmessage=>", err);
              }
            };
          });
          wspo.resolve(ws);
        } catch (err) {
          console.error("WebSocketIpcBuilder", this.#reAcc, err);
          await new Promise((cb) => setTimeout(cb, 3000));
        }
      }
    }
    return wspo.promise;
  }
}
