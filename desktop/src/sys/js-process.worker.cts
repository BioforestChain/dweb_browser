/// <reference lib="webworker"/>
/// 该文件是给 js-worker 用的，worker 中是纯粹的一个runtime，没有复杂的 import 功能，所以这里要极力克制使用外部包。
/// import 功能需要 chrome-80 才支持。我们明年再支持 import 吧，在此之前只能用 bundle 方案来解决问题
import {
  fetch_helpers,
  normalizeFetchArgs,
  $readRequestAsIpcRequest,
} from "../core/helper.cjs";
import { IPC_ROLE } from "../core/ipc.cjs";
import { NativeIpc } from "../core/ipc.native.cjs";
import type { $MicroModule, $MMID } from "../core/types.cjs";

/// 这个文件是给所有的 js-worker 用的，所以会重写全局的 fetch 函数，思路与 dns 模块一致
/// 如果是在原生的系统中，不需要重写fetch函数，因为底层那边可以直接捕捉 fetch
/// 虽然 nwjs 可以通过 chrome.webRequest 来捕捉请求，但没法自定义相应内容
/// 所以这里的方案还是对 fetch 进行重写
/// 拦截到的 ipc-message 通过 postMessage 转发到 html 层，再有 html 层

/**
 * 安装上下文
 */
export const installEnv = (mmid: $MMID) => {
  const process = new (class JsProcessMicroModule implements $MicroModule {
    mmid = mmid;
    fetch(input: RequestInfo | URL, init?: RequestInit) {
      return Object.assign(fetch(input, init), fetch_helpers);
    }
  })();
  /// 消息通道构造器
  self.addEventListener("message", (event) => {
    if (Array.isArray(event.data) && event.data[0] === "ipc-channel") {
      const ipc = new NativeIpc(event.data[1], process, IPC_ROLE.SERVER);
      self.dispatchEvent(new MessageEvent("connect", { data: ipc }));
    }
  });

  /// 初始化内定的主消息通道
  const channel = new MessageChannel();
  const { port1, port2 } = channel;
  self.postMessage(["fetch-ipc-channel", port2], [port2]);
  const fetchIpc: NativeIpc = new NativeIpc(port1, process, IPC_ROLE.SERVER);

  const native_fetch = globalThis.fetch;
  function fetch(url: RequestInfo | URL, init?: RequestInit) {
    const args = normalizeFetchArgs(url, init);
    const { parsed_url } = args;
    /// 进入特殊的解析模式
    if (
      parsed_url.protocol === "file:" &&
      parsed_url.hostname.endsWith(".dweb")
    ) {
      return (async () => {
        const ipc_req_init = await $readRequestAsIpcRequest(args.request_init);
        const ipc_response = await fetchIpc.request(
          parsed_url.href,
          ipc_req_init
        );
        return ipc_response.asResponse();
      })();
    }

    return native_fetch(url, init);
  }
  Object.assign(globalThis, {
    fetch,
    process,
  });
  return process;
};
