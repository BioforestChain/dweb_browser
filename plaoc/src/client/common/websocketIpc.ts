import { ReadableStreamIpc } from "dweb/core/ipc-web/index.ts";
import type { $MicroModuleManifest } from "dweb/core/types.ts";
import { PromiseOut } from "dweb/helper/PromiseOut.ts";
import { simpleEncoder } from "dweb/helper/encoding.ts";
import { ReadableStreamOut, streamReadAll } from "dweb/helper/stream/readableStreamHelper.ts";

// 回复信息给后端
export const createMockModuleServerIpc: (wsUrl: URL, remote: $MicroModuleManifest) => Promise<ReadableStreamIpc> = (
  wsUrl: URL,
  remote: $MicroModuleManifest
) => {
  const waitOpenPo = new PromiseOut<ReadableStreamIpc>();

  /// 通过ws链接到代理服务器
  const ws = new WebSocket(wsUrl);
  ws.binaryType = "arraybuffer";

  ws.onerror = (event) => {
    waitOpenPo.reject(event);
  };
  ws.onopen = () => {
    /**
     * 构建服务器响应器
     */
    const serverIpc = new ReadableStreamIpc(
      remote,
      //@ts-ignore
      "client"
    );
    waitOpenPo.resolve(serverIpc);
    /**
     * 这是来自代理服务器的流对象
     */
    const proxyStream = new ReadableStreamOut<Uint8Array>({ highWaterMark: 0 });
    // 将代理服务器收到的客户端请求，绑定到 响应服务器中
    serverIpc.bindIncomeStream(proxyStream.stream);

    /// 客户端关闭，代理层跟着关闭，所以这里响应服务器也将关闭
    ws.onclose = () => {
      proxyStream.controller.close();
      serverIpc.close(); // 注意，这两个流（proxyStream/serverIpc）并不是其中一个关闭另外一个就立刻关闭。但这里却是是这样的逻辑
    };
    waitOpenPo.onError((event) => {
      proxyStream.controller.error((event as ErrorEvent).error);
    });

    /// 客户端发起请求，代理层传递IPC协议内容，响应服务器将这些数据写入proxyStream，然后会进行解析
    ws.onmessage = (event) => {
      try {
        const data = event.data;
        if (typeof data === "string") {
          proxyStream.controller.enqueue(simpleEncoder(data, "utf8"));
        } else if (data instanceof ArrayBuffer) {
          proxyStream.controller.enqueue(new Uint8Array(data));
        } else {
          throw new Error("should not happend");
        }
      } catch (err) {
        console.error("onmessage=>", err);
      }
    };
    /// 响应服务器将响应内容写入serverIpc，这里读取写入的内容，将响应的内容通过代理层传回
    void streamReadAll(serverIpc.stream, {
      map(chunk) {
        ws.send(chunk);
      },
      complete() {
        console.log("streamReadAll=>", "complete");
        /// 服务器关闭
        ws.close();
      },
    });
  };

  return waitOpenPo.promise;
};
