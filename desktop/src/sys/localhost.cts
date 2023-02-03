import { Ipc, IPC_DATA_TYPE } from "../core/ipc.cjs";
import { NativeMicroModule } from "../core/micro-module.native.cjs";

import http from "node:http";
import {
  $isMatchReq,
  $ReqMatcher,
  PromiseOut,
  simpleEncoder,
} from "../core/helper.cjs";
import type { $Method } from "../core/types.cjs";

interface $OnRequestBind {
  paths: readonly $ReqMatcher[];
  streamController: ReadableByteStreamController;
}

export interface $HttpRequestInfo {
  http_req_id: number;
  url: string;
  method: $Method;
  rawHeaders: string[];
}

export interface $HttpResponseInfo {
  http_req_id: number;
  statusCode: number;
  headers: Record<string, string>;
  body: string | Uint8Array | ReadableStream<Uint8Array | string>;
}

class HttpListener {
  constructor(
    readonly ipc: Ipc,
    readonly port: number,
    readonly host: string
  ) {}

  readonly origin = `http://${this.host}`;
  readonly onRequestBinds = new Set<$OnRequestBind>();

  private _httpReqresMap = new Map<number, PromiseOut<$HttpResponseInfo>>();
  private _http_req_id_acc = 0;
  allocHttpReqId() {
    return this._http_req_id_acc++;
  }
  /** 自定义注册 请求与响应 的id */
  registerHttpReqId(http_req_id = this.allocHttpReqId()) {
    const response_po = new PromiseOut<$HttpResponseInfo>();
    this._httpReqresMap.set(http_req_id, response_po);
    return response_po;
  }
  /** 响应 */
  resolveResponse(response_info: $HttpResponseInfo) {
    const response_po = this._httpReqresMap.get(response_info.http_req_id);
    if (response_po === undefined) {
      throw new Error(
        `no found http request/response by id: ${response_info.http_req_id}`
      );
    }
    this._httpReqresMap.delete(response_info.http_req_id);
    response_po.resolve(response_info);
  }

  private _isBindMatchReq(pathname: string, method: string) {
    for (const bind of this.onRequestBinds) {
      for (const pathMatcher of bind.paths) {
        if ($isMatchReq(pathMatcher, pathname, method)) {
          return { bind, pathMatcher };
        }
      }
    }
  }

  /** 接收处理 nodejs-http 请求 */
  async hookHttpRequest(req: http.IncomingMessage, res: http.ServerResponse) {
    if (res.closed) {
      throw new Error("http server response already closed");
    }

    const { url = "/", method = "GET" } = req;
    const parsed_url = new URL(req.url ?? "/", "http://locaclhost");

    const hasMatch = this._isBindMatchReq(parsed_url.pathname, method);
    if (hasMatch === undefined) {
      defaultErrorPage(req, res, 404, "no found");
      return;
    }

    const http_req_id = this.allocHttpReqId();

    const request_info: $HttpRequestInfo = {
      http_req_id: http_req_id,
      url: url,
      method: method as $Method,
      rawHeaders: req.rawHeaders,
    };

    const chunk = simpleEncoder(JSON.stringify(request_info) + "\n", "utf8");
    /// 通过 `/request/on` 管道将请求通知过去
    hasMatch.bind.streamController.enqueue(chunk);

    /// 然后按需将 body 对象也传回去
    const read_request_body_off = this.ipc.onMessage((message) => {
      if (
        message.type === IPC_DATA_TYPE.REQUEST &&
        message.parsed_url.pathname === "/read-request-body"
      ) {
        read_request_body_off();
      }
    });
    /// 等待远端提供响应
    const response_info = await this.registerHttpReqId(http_req_id).promise;
    /// 写回 res 对象
    res.statusCode = response_info.statusCode;
    for (const [name, value] of Object.entries(response_info.headers)) {
      res.setHeader(name, value);
    }
    res.end(response_info.body);

    /// 移除相关的绑定
    read_request_body_off();
  }
}
/**
 * 这是一个模拟监听本地网络的服务，用来为内部程序提供 https 域名来访问网页的功能
 *
 */
export class LocalhostNMM extends NativeMicroModule {
  mmid = "localhost.sys.dweb" as const;
  private listenMap = new Map</* host */ string, HttpListener>();
  private _local_port = 18909;
  private _http_server?: http.Server;

  async _bootstrap() {
    /// nwjs 拦截到请求后不允许直接构建 Response，即便重定向了也不是 302 重定向。
    /// 所以这里我们直接使用 localhost 作为顶级域名来相应实现相关功能，也就意味着这里需要监听端口
    /// 但在IOS和Android上，也可以听过监听端口来实现类似功能，但所有请求都需要校验
    this._http_server = http
      .createServer((req, res) => {
        const host = req.headers.host;
        if (host == null) {
          defaultErrorPage(req, res, 502, "request host no found");
          return;
        }
        const listener = this.listenMap.get(host);
        if (listener == null) {
          defaultErrorPage(req, res, 502, "invalid gateway");
          return;
        }
        listener.hookHttpRequest(req, res);
      })
      .listen(this._local_port);

    this.registerCommonIpcOnMessageHanlder({
      pathname: "/listen",
      matchMode: "full",
      input: { port: "number" },
      output: { origin: "string" },
      hanlder: async (args, ipc) => {
        return await this.listen(ipc, args.port);
      },
    });
    /// 监听请求，同时配置了过滤器，这样可以多个线程分开响应不同的任务
    this.registerCommonIpcOnMessageHanlder({
      pathname: "/request/on",
      matchMode: "full",
      input: { port: "number", paths: "object" },
      output: "object",
      hanlder: (args, ipc) => {
        return this.onRequest(ipc, args.port, args.paths as any);
      },
    });
    this.registerCommonIpcOnMessageHanlder({
      pathname: "/response/emit",
      method: "POST",
      matchMode: "full",
      input: {
        port: "number",
        http_req_id: "number",
        statusCode: "number",
        headers: "object?",
      },
      output: "void",
      hanlder: (args, ipc, ipc_request) => {
        return this.emitResponse(
          ipc,
          args.port,
          args.http_req_id,
          args.statusCode,
          args.headers as Record<string, string>,
          ipc_request.body
        );
      },
    });
    this.registerCommonIpcOnMessageHanlder({
      pathname: "/unregister",
      matchMode: "full",
      input: { port: "number" },
      output: "boolean",
      hanlder: async (args, ipc) => {
        return await this.unlisten(ipc, args.port);
      },
    });
  }
  _shutdown() {
    this._http_server?.close();
    this._http_server = undefined;
  }

  private _getOrigin(port: number, ipc: Ipc) {
    return `${ipc.module.mmid}.${port}.localhost:${this._local_port}`;
  }

  /// 监听
  listen(ipc: Ipc, port: number) {
    const host = this._getOrigin(port, ipc);
    if (this.listenMap.has(host)) {
      throw new Error(`already in listen with port: ${port}`);
    }
    const origin = `http://${host}`;
    this.listenMap.set(host, new HttpListener(ipc, port, host));
    return { origin, host };
  }

  /** 远端监听请求，将提供一个 jsonlines 流 */
  onRequest(ipc: Ipc, port: number, paths: $ReqMatcher[]) {
    const host = this._getOrigin(port, ipc);
    const listener = this.listenMap.get(host);
    if (listener === undefined) {
      throw new Error(`no listen with port: ${port}`);
    }

    const stream = new ReadableStream<ArrayBufferView>({
      start(controller) {
        const bind: $OnRequestBind = {
          paths,
          streamController: controller as ReadableByteStreamController,
        };
        listener.onRequestBinds.add(bind);
      },
    });
    return new Response(stream, { status: 200 });
  }
  /** 远端响应请求 */
  emitResponse(
    ipc: Ipc,
    port: number,
    http_req_id: number,
    statusCode: number,
    headers: Record<string, string>,
    body: string | Uint8Array | ReadableStream<Uint8Array | string>
  ) {
    const host = this._getOrigin(port, ipc);
    const listener = this.listenMap.get(host);
    if (listener === undefined) {
      throw new Error(`no listen with port: ${port}`);
    }

    listener.resolveResponse({
      http_req_id,
      statusCode,
      body,
      headers,
    });
  }
  /**
   * 释放监听
   */
  unlisten(ipc: Ipc, port: number) {
    const host = this._getOrigin(port, ipc);
    return this.listenMap.delete(host);
  }

  // $Routers: {
  //    "/register": IO<mmid, boolean>;
  //    "/unregister": IO<mmid, boolean>;
  // };
}

/**
 * 这是默认的错误页
 *
 * /// TODO 将会开放接口来让开发者可以自定义错误页的模板内容，也可以基于错误码范围进行精确匹配
 *
 * @param req
 * @param res
 * @param statusCode
 * @param errorMessage
 * @returns
 */
export const defaultErrorPage = (
  req: http.IncomingMessage,
  res: http.ServerResponse,
  statusCode: number,
  errorMessage: string
) => {
  // let body = "";
  // if (req.method === "POST" || req.method === "PUT") {
  //   body = await new Promise<string>((resolve) => {
  //     const chunks: Uint8Array[] = [];
  //     req.on("data", (chunk) => {
  //       chunks.push(Buffer.from(chunk));
  //     });
  //     req.on("end", () => {
  //       resolve(Buffer.concat(chunks).toString("utf-8"));
  //     });
  //   });
  // }
  const html = String.raw;
  const body = html`
    <!DOCTYPE html>
    <html lang="en">
      <head>
        <meta charset="UTF-8" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>${statusCode}</title>
      </head>
      <body>
        <h1>CODE: ${statusCode}</h1>
        <pre style="color:red">${errorMessage}</pre>
        <p><b>URL:</b>${req.url}</p>
        <p><b>METHOD:</b>${req.method}</p>
        <p><b>HEADERS:</b>${req.rawHeaders}</p>
      </body>
    </html>
  `;
  // <p><b>BODY:</b>${body}</p>

  res.statusCode = statusCode;
  res.end(body);
  return body;
};
