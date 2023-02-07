import {
  Ipc,
  IpcRequest,
  IPC_DATA_TYPE,
  IPC_ROLE,
} from "../core/ipc/index.cjs";
import { NativeMicroModule } from "../core/micro-module.native.cjs";

// import node_web from "node:http2";
// const protocol = "https:";
// import WebServer = node_web.Http2Server;
// import WebServerRequest = node_web.Http2ServerRequest;
// import WebServerResponse = node_web.Http2ServerResponse;

import node_web from "node:http";
const protocol = "http:";
import WebServer = node_web.Server;
import WebServerRequest = node_web.IncomingMessage;
import WebServerResponse = node_web.ServerResponse;

import { Readable } from "node:stream";
import { ReadableStreamIpc } from "../core/ipc-web/ReadableStreamIpc.cjs";
import { $isMatchReq, $ReqMatcher } from "../helper/$ReqMatcher.cjs";
import { findPort } from "../helper/findPort.cjs";
import { PromiseOut } from "../helper/PromiseOut.cjs";
import {
  ReadableStreamOut,
  streamFromCallback,
  streamReader,
} from "../helper/readableStreamHelper.cjs";
import type { $Method } from "../helper/types.cjs";
import { parseUrl } from "../helper/urlHelper.cjs";

interface $OnRequestBind {
  paths: readonly $ReqMatcher[];
  httpResponseIpc: ReadableStreamIpc;
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

  readonly origin = `${protocol}//${this.host}`;
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
  async hookHttpRequest(req: WebServerRequest, res: WebServerResponse) {
    if (res.closed) {
      throw new Error("http server response already closed");
    }

    const { url = "/", method = "GET" } = req;
    const parsed_url = parseUrl(req.url ?? "/");

    const hasMatch = this._isBindMatchReq(parsed_url.pathname, method);
    if (hasMatch === undefined) {
      defaultErrorPage(req, res, 404, "no found");
      return;
    }

    let ipc_req_body_stream: undefined | ReadableStream<Uint8Array>;
    /// 如果是存在body的协议，那么将之读取出来
    console.log("req.method", req.method, "req.readable", req.readable);
    if (req.method === "POST" || req.method === "PUT") {
      /** req body 的转发管道，转发到 响应服务端 */
      const ipc_req_body_piper = new ReadableStreamOut<Uint8Array>();
      (async () => {
        const req_body_reader = Readable.toWeb(req).getReader();

        /// 根据数据拉取的情况，从 req 中按需读取数据，这种按需读取会反压到 web 的请求层那边暂缓数据的发送
        for await (const _ of streamReader(
          streamFromCallback(ipc_req_body_piper.onPull)
        )) {
          const item = await req_body_reader.read();
          if (item.done) {
            /// 客户端的传输一旦关闭，转发管道也要关闭
            ipc_req_body_piper.controller.close();
          } else {
            ipc_req_body_piper.controller.enqueue(item.value);
          }
        }
      })();

      ipc_req_body_stream = ipc_req_body_piper.stream;
    }

    const http_response_info = await hasMatch.bind.httpResponseIpc.request(
      url,
      {
        method,
        body: ipc_req_body_stream,
        headers: req.headers as Record<string, string>,
      }
    );

    /// 写回 res 对象
    res.statusCode = http_response_info.statusCode;
    for (const [name, value] of Object.entries(http_response_info.headers)) {
      res.setHeader(name, value);
    }
    /// 204 和 304 不可以包含 body
    if (
      http_response_info.statusCode !== 204 &&
      http_response_info.statusCode !== 304
    ) {
      // await (await http_response_info.stream()).pipeTo(res)
      const http_response_body = http_response_info.body;
      if (http_response_body instanceof ReadableStream) {
        streamReader(http_response_body).each(
          (chunk) => res.write(chunk),
          () => res.end()
        );
      } else {
        res.end(http_response_body);
        // res.end();/// nw.js 调用 http2 end 会导致 nw 崩溃死掉？
      }
    }
  }
}
/**
 * 这是一个模拟监听本地网络的服务，用来为内部程序提供 https://*.localhost.dweb 域名，并提供请求转发
 *
 * ### 实现原理：
 *
 * 我们在原生端静态地提供了 https://*.localhost.dweb/(index.html|sw.js) 等请求的响应。
 * 这些响应是内容是写死的，所以即便我们是通过 https-server 来提供服务，因为只是纯静态服务，也不会有任何风险。
 * 服务起来后，service-worker 将会运行，然后拦截所有 https://*.localhost.dweb/ 网站下的请求，
 * 再通过内部 IPC 转发到响应服务中进行处理。
 *
 * ### 如何实现 https://*.localhost.dweb 站点服务：
 *
 * 在 Android 端，我们使用 `webViewClient.shouldInterceptRequest` 接口来提供网站响应请求。
 *
 * 在 IOS 端，通过 `WKWebURLSchemeHandler` 类来提供网站响应请求（需要使用Object-C重写 handlesURLScheme 函数来绕过 拦截https 的限制）。
 *
 * 在 Desktop（Win/Mac/Linux） 端，我们使用通过自定义 CA 证书，并通过 chrome.proxy 来配置代理服务。
 * 在本地启动 https 的服务来作为代理服务器，为 *localhost.dweb 动态生成证书。从而使用标准 https 协议来响应网站请求。
 *
 * ### 如何实现 https://*.localhost.deb 的请求拦截给 js-process 处理的功能
 *
 * 在 Android 端，因为我们使用了service-worker得到了请求拦截的能力，所以本质上我们需要跟 service-worker 建立ipc通讯。
 * 这里我们使用 `webView.createWebMessageChannel` 来创建原生的 MessagePort，
 * 然后通过 `webView.postWebMessage(new WebMessage("forward-to-service-worker", new WebMessagePort[]{port[1]}));` 来传入Web端（Web端通常指 web的主线程 ）。
 * Web端使用 `window.addEventListener('message')` 来接收这个 channel，之后再将之转发给 service-Worker: `serviceWorkerController.postMessage("from-native", [port2])`。
 * 从而实现了 Native 与 Service-Worker 的直接通讯（字符串通讯，这里为了JS侧的性能，我们使用JSON格式进行通讯，二进制数据则会转换成base64）
 * 最后再通过 js-process，将这个 Native 侧的IPC 转发给内部的 WebWorker 即可。
 *
 * 在 IOS 端，因为不支持Service-Worker（强行支持就无法使用 WKWebURLSchemeHandler 拦截请求了），所以我们在拦截到如果是存在 request.body （比如 POST、PUT），需要额外通过 `WKNavigationDelegate` 取得 request.body 的二进制或者二进制流。
 * 所以将这些请求封装成 Native 侧的 IpcRequest ，交给 js-process 让其转发给内部的 WebWorker 即可。
 *
 * 在 Desktop（Win/Mac/Linux） 端，我们虽然已经有自启动的端口服务，但为了确保安全，我们不会使用它来做网络拦截。
 * 而是与 Android 采用类似的方案，直接往 service-worker 中通过传入 MessagePort 来直接进行通讯。（此时的通讯直接传输 Transferable 对象，不需要JSON编码，性能会更进一步）
 * 后面的数据路径都一样，给到 js-process 来让其转发给内部的 WebWorker
 */
export class LocalhostNMM extends NativeMicroModule {
  mmid = "localhost.sys.dweb" as const;
  private listenMap = new Map</* host */ string, HttpListener>();
  private _local_port = 0;
  private _server?: WebServer;

  async _bootstrap() {
    /// nwjs 拦截到请求后不允许直接构建 Response，即便重定向了也不是 302 重定向。
    /// 所以这里我们直接使用 localhost 作为顶级域名来相应实现相关功能，也就意味着这里需要监听端口
    /// 但在IOS和Android上，也可以听过监听端口来实现类似功能，但所有请求都需要校验
    this._server = node_web
      // .createSecureServer({
      //   // allowHTTP1: true,
      //   key: await fetch("../../cert/_wildcard.localhost-key.pem").then((res) =>
      //     res.text()
      //   ),
      //   cert: await fetch("../../cert/_wildcard.localhost.pem").then((res) =>
      //     res.text()
      //   ),
      // })
      .createServer()
      .listen((this._local_port = await findPort([28909])))
      .addListener("request", (req, res) => {
        const host = (req.headers.host || req.headers[":authority"]) as string;
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
      });

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
    // this.registerCommonIpcOnMessageHanlder({
    //   pathname: "/request/on",
    //   matchMode: "full",
    //   input: { port: "number", paths: "object" },
    //   output: "object",
    //   hanlder: (args, ipc) => {
    //     return this.onRequest(ipc, args.port, args.paths as any);
    //   },
    // });
    this.registerCommonIpcOnMessageHanlder({
      method: "POST",
      pathname: "/request/on",
      matchMode: "full",
      input: { port: "number", paths: "object" },
      output: "object",
      hanlder: async (args, ipc, message) => {
        console.log("收到处理请求的双工通道");
        return this.onRequest(ipc, message, args.port, args.paths as any);
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
    this._server?.close();
    this._server = undefined;
  }

  private _getHost(port: number, ipc: Ipc) {
    return `${ipc.remote.mmid}.${port}.localhost:${this._local_port}`;
  }

  /// 监听
  listen(ipc: Ipc, port: number) {
    const host = this._getHost(port, ipc);
    if (this.listenMap.has(host)) {
      throw new Error(`already in listen with port: ${port}`);
    }
    const origin = `${protocol}//${host}`;
    this.listenMap.set(host, new HttpListener(ipc, port, host));
    return { origin, host };
  }

  /** 远端监听请求，将提供一个 jsonlines 流 */
  async onRequest(
    ipc: Ipc,
    message: IpcRequest,
    port: number,
    paths: $ReqMatcher[]
  ) {
    const host = this._getHost(port, ipc);
    const listener = this.listenMap.get(host);
    if (listener === undefined) {
      throw new Error(`no listen with port: ${port}`);
    }

    const httpResponseIpc = new ReadableStreamIpc(ipc.remote, IPC_ROLE.CLIENT);
    httpResponseIpc.bindIncomeStream(await message.stream());
    httpResponseIpc.onMessage((response) => {
      if (response.type === IPC_DATA_TYPE.RESPONSE) {
      }
    });

    const bind: $OnRequestBind = {
      paths,
      httpResponseIpc,
    };
    listener.onRequestBinds.add(bind);
    return new Response(httpResponseIpc.stream, { status: 200 });
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
    const host = this._getHost(port, ipc);
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
    const host = this._getHost(port, ipc);
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
  req: WebServerRequest,
  res: WebServerResponse,
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
