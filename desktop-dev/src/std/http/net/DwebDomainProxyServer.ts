import http from "node:http";
import net from "node:net";
import { findPort } from "../../../helper/findPort.ts";
import type { HttpsServer } from "./HttpsServer.ts";
import { $HttpServerInfo } from "./createNetServer.ts";

export class DwebDomainProxyServer {
  private proxyPort = -1;

  get host() {
    return `http://127.0.0.1:${this.proxyPort}`;
  }

  private _proxy?: $HttpServerInfo;
  get proxy() {
    return this._proxy;
  }

  async createProxyServer(gatewayServer: HttpsServer) {
    this.proxyPort = await findPort([(this.proxyPort = 22600)]);
    const gatewayHostname = gatewayServer.info?.hostname ?? "localhost";
    const gatewayPort = gatewayServer.info?.port ?? 22605;

    const proxyServer = http.createServer({});
    process.env["NODE_TLS_REJECT_UNAUTHORIZED"] = "0";

    await new Promise<void>((resolve, reject) => {
      proxyServer
        .on("connect", (req, socket, head) => {
          const [hostname, port] = req.url!.split(":");

          const doProxy = (port: number, host: string) => {
            const targetSocket = net.connect(port, host, () => {
              socket.write("HTTP/1.1 200 Connection Established\r\n\r\n");
              targetSocket.write(head);
              targetSocket.pipe(socket).pipe(targetSocket);
            });
            // 监听错误事件
            socket.on("error", (err) => {
              console.warn("Client socket error:", err);
            });
            targetSocket.on("error", (err) => {
              console.warn("Server socket error:", err);
            });
          };

          // console.always("createProxyServer", hostname);
          if (hostname.endsWith(".dweb")) {
            doProxy(gatewayPort, gatewayHostname);
          } else if (port) {
            doProxy(parseInt(port), hostname);
          } else {
            socket.end("HTTP/1.1 400 Bad Request\r\n\r\n");
          }
        })
        .on("error", reject)
        .listen(this.proxyPort, "127.0.0.1", resolve);
    });

    // return proxyServer
  }
}
