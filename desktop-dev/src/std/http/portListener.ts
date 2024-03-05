import { $isMatchReq, $ReqMatcher } from "../../core/helper/$ReqMatcher.ts";
import type { ReadableStreamIpc } from "../../core/ipc/ReadableStreamIpc.ts";
import type { Ipc } from "../../core/ipc/ipc.ts";
import { createSignal } from "../../helper/createSignal.ts";
import { httpMethodCanOwnBody } from "../../helper/httpHelper.ts";
import { readableToWeb } from "../../helper/stream/nodejsStreamHelper.ts";
import { streamReadAll } from "../../helper/stream/readableStreamHelper.ts";
import { parseUrl } from "../../helper/urlHelper.ts";
import { defaultErrorResponse } from "./defaultErrorResponse.ts";
import type { WebServerRequest, WebServerResponse } from "./types.ts";

export interface $Router {
  routes: readonly $ReqMatcher[];
  streamIpc: ReadableStreamIpc;
}

/**
 * > 目前只允许端口独占，未来会开放共享监听以及对应的路由策略（比如允许开发WASM版本的路由策略）
 */
export class PortListener {
  constructor(readonly ipc: Ipc, readonly host: string, readonly origin: string) {}

  protected _routers = new Set<$Router>();
  addRouter(router: $Router) {
    this._routers.add(router);
    return () => {
      this._routers.delete(router);
    };
  }

  /**
   * 获取绑定的路有处理器
   * @param pathname
   * @param method
   * @returns
   */
  findMatchedBind(pathname: string, method: string) {
    for (const bind of this._routers) {
      for (const pathMatcher of bind.routes) {
        if ($isMatchReq(pathMatcher, pathname, method)) {
          return { bind, pathMatcher };
        }
      }
    }
  }

  /**
   * 接收 nodejs-web 请求
   * 将之转发给 IPC 处理，等待远端处理完成再代理响应回去
   */
  async hookHttpRequest(req: WebServerRequest, res: WebServerResponse, fullReqUrl: string) {
    const { method = "GET" } = req;
    const parsed_url = parseUrl(fullReqUrl, this.origin);
    const hasMatch = this.findMatchedBind(parsed_url.pathname, method);
    if (hasMatch === undefined) {
      defaultErrorResponse(req, res, 404, "no found");
      return;
    }

    // console.always("接受到了请求", parsed_url.href);

    /**
     * 要通过 ipc 传输过去的 req.body
     *
     * 这里采用按需传输给远端（server 端发来 pull 指令时），
     * 同时如果关闭连接，这类也会关闭 client 的 req 的流连接
     * 反之亦然，如果 req 主动关闭了流连接， 我们也会关闭这个 stream
     */
    let ipc_req_body_stream: undefined | ReadableStream<Uint8Array>;
    /// 如果是存在body的协议，那么将之读取出来
    /// 参考文档 https://www.rfc-editor.org/rfc/rfc9110.html#name-method-definitions
    if (
      /// 理论上除了 GET/HEAD/OPTIONS 之外的method （比如 DELETE）是允许包含 BODY 的，但这类严格的对其进行限制，未来可以通过启动监听时的配置来解除限制
      httpMethodCanOwnBody(method)
    ) {
      /** req body 的转发管道，转发到 响应服务端 */
      ipc_req_body_stream = readableToWeb(req, {
        strategy: {
          // 时刻反压
          highWaterMark: 0,
        },
      });
    }
    // console.log('http/port-listener',`分发消息 http://${req.headers.host}${url}`);
    // 分发消息
    const ipcResponse = await hasMatch.bind.streamIpc.request(fullReqUrl, {
      method,
      body: ipc_req_body_stream,
      headers: req.headers as Record<string, string>,
    });

    /// 写回 res 对象
    res.statusCode = ipcResponse.statusCode;
    ipcResponse.headers.forEach((value, name) => {
      res.setHeader(name, value);
    });
    /// 204 和 304 不可以包含 body
    if (ipcResponse.statusCode !== 204 && ipcResponse.statusCode !== 304) {
      // await (await http_response_info.stream()).pipeTo(res)
      const http_response_body = ipcResponse.body.raw;
      if (http_response_body instanceof ReadableStream) {
        void streamReadAll(http_response_body, {
          map(chunk) {
            res.write(chunk);
          },
          complete() {
            res.end();
          },
        });
      } else {
        res.end(http_response_body);
        // res.end();/// nw.js 调用 http2 end 会导致 nw 崩溃死掉？
      }
    }
  }

  private _on_destroy_signal = createSignal<() => unknown>();
  /** 监听 destroy 时间 */
  onDestroy = this._on_destroy_signal.listen;
  /** 销毁监听器内产生的引用 */
  destroy() {
    Array.from(this._routers).map((item) => item.streamIpc.close());
    // 删除 Router 保存的IPC
    this._on_destroy_signal.emit();
  }
}
