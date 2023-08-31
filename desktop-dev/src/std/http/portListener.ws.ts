import { Server as HttpServer, IncomingMessage } from "node:http";
import { FetchError } from "../../core/helper/ipcFetchHelper.ts";
import { simpleEncoder } from "../../helper/encoding.ts";
import { ReadableStreamOut, streamReadAll } from "../../helper/stream/readableStreamHelper.ts";
import { parseUrl } from "../../helper/urlHelper.ts";
import { WebSocketClient, WebSocketServer } from "../../helper/websocketServerHelper.ts";
import { formatErrorToHtml } from "./defaultErrorResponse.ts";
import type { HttpServerNMM } from "./http.nmm.ts";
import type { WebSocketDuplex } from "./types.ts";

export function initWebSocketServer(this: HttpServerNMM, server: HttpServer) {
  const getHostByReq = (req: IncomingMessage) => this.getHostByReq(req.url, Object.entries(req.headers));
  const getFullReqUrl = (req: IncomingMessage) => this.getFullReqUrl(req);
  const getGateway = (host: string) => this._gatewayMap.get(host);
  type $OnConnection = (client: InstanceType<typeof WebSocketClient>, request: IncomingMessage) => void;

  class MyWebSocketServer extends WebSocketServer {
    override handleUpgrade(
      req: IncomingMessage,
      socket: WebSocketDuplex,
      upgradeHead: Buffer,
      callback: $OnConnection
    ) {
      const fullReqUrl = getFullReqUrl(req);
      try {
        /// 网关校验
        const host = getHostByReq(req);
        const gateway = getGateway(host);
        if (gateway === undefined) {
          return false;
        }

        /// 路由校验
        const { method = "GET" } = req;
        const parsed_url = parseUrl(req.url ?? "/", gateway.listener.origin);
        const hasMatch = gateway.listener.findMatchedBind(parsed_url.pathname, method);
        if (hasMatch) {
          const onConnnection: $OnConnection = (ws, req) => {
            ws.binaryType = "nodebuffer";
            callback(ws, req);

            /**
             * 要通过 ipc 传输过去的 readableStream
             */
            const inputStreamOut = new ReadableStreamOut<Uint8Array>({
              highWaterMark: 0,
            });

            /// 创建 ipcResponse 来为 websocket 实例填充数据
            void (async () => {
              // 转发消息到处理层
              const outputResponse = await hasMatch.bind.streamIpc.request(fullReqUrl, {
                method: req.method,
                body: inputStreamOut.stream,
                headers: req.headers as Record<string, string>,
              });

              /// 如果是200响应头，那么使用WebSocket来作为双工的通讯标准进行传输
              if (outputResponse.statusCode === 200) {
                /// 转发来自服务器的数据
                const outputStream = await outputResponse.body.stream();
                //TODO close outputStream
                void streamReadAll(outputStream, {
                  map(chunk) {
                    ws.send(chunk);
                  },
                  complete() {
                    /// 服务端关闭流
                    ws.close();
                  },
                });
              }
              /// 如果是 101 响应头，那么使用标准的WebSocket来进行通讯
              else if (outputResponse.statusCode === 101) {
                /// 转发来自服务器的数据
                const outputStream = await outputResponse.body.stream();
                //TODO close outputStream
                void streamReadAll(outputStream, {
                  map(chunk) {
                    req.socket.write(chunk);
                  },
                  complete() {
                    /// 服务端关闭流
                    req.socket.end();
                  },
                });
              }
              /// 其它情况当作错误处理
              else {
                const data = JSON.stringify({
                  headers: outputResponse.headers.toJSON(),
                  body: await outputResponse.body.text(),
                });
                ws.close(outputResponse.statusCode, data);
              }
            })().catch(console.error);

            /// 传输来自客户端的数据，注意，这个onmessage 前面不要有任何 await，否则会丢失消息
            ws.on("message", (data, isBinary) => {
              const encoded_data = (
                isBinary ? (data as Uint8Array) : simpleEncoder(data as unknown as string, "utf8")
              );
              inputStreamOut.controller.enqueue(encoded_data);
            });
            /// 客户端关闭流
            ws.on("close", () => {
              inputStreamOut.controller.close();
              // console.always("websocket end", fullReqUrl);
            });

            /// 监听异常
            ws.on("error", (reason) => {
              inputStreamOut.controller.error(reason);
            });
          };
          /// 响应 WebSocket
          return super.handleUpgrade(req, socket, upgradeHead, onConnnection);
        }
      } catch (err) {
        if (err instanceof FetchError) {
          abortHandshake(socket, err.code, formatErrorToHtml(err.code, fullReqUrl, req.method, req.headers, err), {
            "Content-Type": "text/html",
          });
        } else {
          abortHandshake(socket, 500, formatErrorToHtml(500, fullReqUrl, req.method, req.headers, err), {
            "Content-Type": "text/html",
          });
        }
      }
    }
  }
  /// 开启 WebSocket 支持
  new MyWebSocketServer({
    server: server,
  });
}

import http from "node:http";
/**
 *
 * Close the connection when preconditions are not fulfilled.
 *
 * https://github.com/websockets/ws/blob/master/lib/websocket-server.js#L533
 *
 * @param {(net.Socket|tls.Socket)} socket The socket of the upgrade request
 * @param {Number} code The HTTP response status code
 * @param {String} [message] The HTTP response body
 * @param {Object} [headers] Additional HTTP response headers
 * @private
 */
function abortHandshake(
  socket: WebSocketDuplex,
  code: number,
  message?: string,
  headers: Record<string, string | number> = {}
) {
  //
  // The socket is writable unless the user destroyed or ended it before calling
  // `server.handleUpgrade()` or in the `verifyClient` function, which is a user
  // error. Handling this does not make much sense as the worst that can happen
  // is that some of the data written by the user might be discarded due to the
  // call to `socket.end()` below, which triggers an `'error'` event that in
  // turn causes the socket to be destroyed.
  //
  message = message || http.STATUS_CODES[code] || "";
  headers = {
    Connection: "close",
    "Content-Type": "text/html",
    "Content-Length": Buffer.byteLength(message),
    ...headers,
  };

  socket.once("finish", socket.destroy);

  socket.end(
    `HTTP/1.1 ${code} ${http.STATUS_CODES[code]}\r\n` +
      Object.keys(headers)
        .map((h) => `${h}: ${headers[h]}`)
        .join("\r\n") +
      "\r\n\r\n" +
      message
  );
}

const keyRegex = /^[+/0-9A-Za-z]{22}==$/;
