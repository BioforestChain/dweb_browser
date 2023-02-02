/// <reference lib="webworker"/>
/// 该文件是给 js-worker 用的，worker 中是纯粹的一个runtime，没有复杂的 import 功能，所以这里要极力克制使用外部包。
/// import 功能需要 chrome-80 才支持。我们明年再支持 import 吧，在此之前只能用 bundle 方案来解决问题
import {
  normalizeFetchArgs,
  PromiseOut,
  readRequestAsIpcRequest,
} from "../core/helper.cjs";
import { IpcRequest, IpcResponse, IPC_DATA_TYPE } from "../core/ipc.cjs";
import { $messageToIpcMessage, NativeIpc } from "../core/ipc.native.cjs";

/// 这个文件是给所有的 js-worker 用的，所以会重写全局的 fetch 函数，思路与 dns 模块一致
/// 如果是在原生的系统中，不需要重写fetch函数，因为底层那边可以直接捕捉 fetch
/// 虽然 nwjs 可以通过 chrome.webRequest 来捕捉请求，但没法自定义相应内容
/// 所以这里的方案还是对 fetch 进行重写
/// 拦截到的 ipc-message 通过 postMessage 转发到 html 层，再有 html 层

/**
 * 安装上下文
 */
export const installEnv = () => {
  /// 消息通道构造器
  self.addEventListener("message", (event) => {
    if (Array.isArray(event.data) && event.data[0] === "ipc-channel") {
      const ipc = new NativeIpc(event.data[1]);
      self.dispatchEvent(new MessageEvent("connect", { data: ipc }));
    }
  });

  /// 初始化内定的主消息通道
  const channel = new MessageChannel();
  const { port1, port2 } = channel;
  self.postMessage(["fetch-ipc-channel", port2], [port2]);
  const fetchIpc = new NativeIpc(port1);
  fetchIpc.onMessage((message) => {
    if (message.type === IPC_DATA_TYPE.RESPONSE) {
      const res_po = reqresMap.get(message.req_id);
      if (res_po !== undefined) {
        reqresMap.delete(message.req_id);
        res_po.resolve(message);
      }
    }
  });

  const reqresMap = new Map<number, PromiseOut<IpcResponse>>();

  let req_id = 0;
  const allocReqId = () => req_id++;

  const native_fetch = globalThis.fetch;
  globalThis.fetch = function fetch(
    url: RequestInfo | URL,
    init?: RequestInit
  ) {
    const args = normalizeFetchArgs(url, init);
    const { parsed_url } = args;
    /// 进入特殊的解析模式
    if (
      parsed_url.protocol === "file:" &&
      parsed_url.hostname.endsWith(".dweb")
    ) {
      return (async () => {
        const { body, method, headers } = await readRequestAsIpcRequest(
          args.request_init
        );

        /// 注册回调
        const req_id = allocReqId();
        const response_po = new PromiseOut<IpcResponse>();
        reqresMap.set(req_id, response_po);

        /// 发送
        fetchIpc.postMessage(
          new IpcRequest(req_id, method, parsed_url.href, body, headers)
        );
        const ipc_response = await response_po.promise;
        return new Response(ipc_response.body, {
          headers: ipc_response.headers,
          status: ipc_response.statusCode,
        });
      })();
    }

    return native_fetch(url, init);
  };
};
