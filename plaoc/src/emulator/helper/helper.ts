import {
  $MMID,
  IPC_ROLE,
  PromiseOut,
  ReadableStreamIpc,
  ReadableStreamOut,
  simpleEncoder,
  streamRead,
} from "../../../deps.ts";
import { X_PLAOC_QUERY } from "../../server/const.ts";
export const EMULATOR = "/emulator";

export function isShadowRoot(o: ShadowRoot | unknown): o is ShadowRoot {
  return typeof o === "object" && o !== null && "host" in o && "mode" in o;
}

export function isHTMLElement(o: HTMLElement | unknown): o is HTMLElement {
  return o instanceof HTMLElement;
}

export function isCSSStyleDeclaration(
  o: CSSStyleDeclaration | unknown
): o is CSSStyleDeclaration {
  return o instanceof CSSStyleDeclaration;
}
export type EmulatorAction = "connect" | "response";

const BASE_URL = new URL(
  new URLSearchParams(location.search)
    .get(X_PLAOC_QUERY.API_INTERNAL_URL)!
    .replace(/^http:/, "ws:")
    .replace(/^https:/, "wss:")
);
BASE_URL.pathname = EMULATOR;

// 回复信息给后端
export const createMockModuleServerIpc = (mmid: $MMID, apiUrl = BASE_URL) => {
  const waitOpenPo = new PromiseOut<ReadableStreamIpc>();

  const wsUrl = new URL(apiUrl);
  wsUrl.searchParams.set("mmid", mmid);
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
      {
        mmid,
        ipc_support_protocols: {
          message_pack: false,
          protobuf: false,
          raw: false,
        },
        dweb_deeplinks: [],
      },
      IPC_ROLE.CLIENT
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
      const data = event.data;
      if (typeof data === "string") {
        proxyStream.controller.enqueue(simpleEncoder(data, "utf8"));
      } else if (data instanceof ArrayBuffer) {
        proxyStream.controller.enqueue(new Uint8Array(data));
      } else {
        throw new Error("should not happend");
      }
    };
    /// 响应服务器将响应内容写入serverIpc，这里读取写入的内容，将响应的内容通过代理层传回
    void (async () => {
      for await (const chunk of streamRead(serverIpc.stream)) {
        ws.send(chunk);
      }
      /// 服务器关闭
      ws.close();
    })();
  };

  return waitOpenPo.promise;
};
