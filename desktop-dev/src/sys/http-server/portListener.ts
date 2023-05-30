import { Readable } from "node:stream";
import type { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.ts";
import type { Ipc } from "../../core/ipc/ipc.ts";
import { $isMatchReq, $ReqMatcher } from "../../helper/$ReqMatcher.ts";
import { createSignal } from "../../helper/createSignal.ts";
import { httpMethodCanOwnBody } from "../../helper/httpMethodCanOwnBody.ts";
import {
  ReadableStreamOut,
  streamFromCallback,
  streamRead,
  streamReadAll,
} from "../../helper/readableStreamHelper.ts";
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
  constructor(
    readonly ipc: Ipc,
    readonly host: string,
    readonly origin: string
  ) {}

  private _routers = new Set<$Router>();
  addRouter(router: $Router) {
    this._routers.add(router);
    return () => {
      this._routers.delete(router);
    };
  }

  /**
   * 判断是否有绑定的请求
   * @param pathname
   * @param method
   * @returns
   */
  private _isBindMatchReq(pathname: string, method: string) {
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
  async hookHttpRequest(req: WebServerRequest, res: WebServerResponse) {
    const { url = "/", method = "GET" } = req;
    const parsed_url = parseUrl(url, this.origin);
    const hasMatch = this._isBindMatchReq(parsed_url.pathname, method);
    if (hasMatch === undefined) {
      defaultErrorResponse(req, res, 404, "no found");
      return;
    }

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
      // &&
      // /// HTTP/1.x 的规范：（我们自己的 file: 参考了该标准）
      // (this.protocol === "http:" || this.protocol === "file:")
      //   ? /// 请求和响应主体要么需要发送 Content-Length 标头，以便另一方知道它将接收多少数据
      //     +(req.headers["content-length"] || 0) > 0 ||
      //     // 要么更改消息格式以使用分块编码。使用分块编码，正文被分成多个部分，每个部分都有自己的内容长度
      //     req.headers["transfer-encoding"] /* ?.includes("chunked") */
      //   : true
    ) {
      /** req body 的转发管道，转发到 响应服务端 */

      const server_req_body_writter = new ReadableStreamOut<Uint8Array>();
      (async () => {
        const client_req_body_reader = Readable.toWeb(req).getReader();
        // 可能出现 数据还没有传递完毕，但是却关闭了
        // client_req_body_reader.closed.then(() => {
        //   server_req_body_writter.controller.close();
        // });
        /// 根据数据拉取的情况，从 req 中按需读取数据，这种按需读取会反压到 web 的请求层那边暂缓数据的发送
        for await (const _ of streamRead(
          streamFromCallback(
            server_req_body_writter.onPull,
            client_req_body_reader.closed
          )
        )) {
          const item = await client_req_body_reader.read();
          if (item.done) {
            /// 客户端的传输一旦关闭，转发管道也要关闭
            server_req_body_writter.controller.close();
            break;
          } else {
            server_req_body_writter.controller.enqueue(item.value);
          }
        }
      })();

      ipc_req_body_stream = server_req_body_writter.stream;
    }
    // console.log('http/port-listener',`分发消息 http://${req.headers.host}${url}`);
    // 分发消息
    const http_response_info = await hasMatch.bind.streamIpc.request(url, {
      method,
      body: ipc_req_body_stream,
      headers: req.headers as Record<string, string>,
    });

    /// 写回 res 对象
    res.statusCode = http_response_info.statusCode;
    http_response_info.headers.forEach((value, name) => {
      res.setHeader(name, value);
    });
    /// 204 和 304 不可以包含 body
    if (
      http_response_info.statusCode !== 204 &&
      http_response_info.statusCode !== 304
    ) {
      // await (await http_response_info.stream()).pipeTo(res)
      const http_response_body = http_response_info.body.raw;
      if (http_response_body instanceof ReadableStream) {
        streamReadAll(http_response_body, {
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
    Array.from(this._routers).map((item) => item.streamIpc.close()); // 停止 streamIpc 是否还有这个必要吗？？
    // 删除 Router 保存的IPC
    this._on_destroy_signal.emit();
  }
}
