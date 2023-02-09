import forge from "node-forge";
import fs from "node:fs";
import http from "node:http";
import https from "node:https";
import net, { AddressInfo } from "node:net";
import tls from "node:tls";
import url from "node:url";
import { createServerCertificate } from "./cert.cjs";

http && https && fs && net && tls && url && forge && createServerCertificate;

function onConnectFactory(targetServerPort: number) {
  return function connect(
    clientRequest: http.IncomingMessage,
    clientSocket: net.Socket,
    head: Buffer
  ) {
    // 连接目标服务器
    const targetSocket = net.connect(targetServerPort, "127.0.0.1", () => {
      // 通知客户端已经建立连接
      clientSocket.write(
        "HTTP/1.1 200 Connection Established\r\n" +
          "Proxy-agent: MITM-proxy\r\n" +
          "\r\n"
      );

      // 建立通信隧道，转发数据
      targetSocket.write(head);
      clientSocket.pipe(targetSocket).pipe(clientSocket);
    });
  };
}

/** 创建支持多域名的 https 服务 **/
function createFakeHttpsServer(fakeServerPort = 0) {
  return new Promise<https.Server>((resolve, reject) => {
    const fakeServer = new https.Server({
      SNICallback: (hostname, callback) => {
        const { key, cert } = createServerCertificate(hostname);
        callback(
          null,
          tls.createSecureContext({
            key: forge.pki.privateKeyToPem(key),
            cert: forge.pki.certificateToPem(cert),
          })
        );
      },
    });
    fakeServer.on("error", reject).listen(fakeServerPort, () => {
      resolve(fakeServer);
    });
  });
}

function createProxyServer(proxyPort: number) {
  return new Promise<https.Server>((resolve, reject) => {
    const serverCrt = createServerCertificate("localhost");
    const proxyServer = https
      .createServer({
        key: forge.pki.privateKeyToPem(serverCrt.key),
        cert: forge.pki.certificateToPem(serverCrt.cert),
      })
      .on("error", reject)
      .listen(proxyPort, () => {
        const proxyUrl = `https://localhost:${proxyPort}`;
        console.log("启动代理成功，代理地址：", proxyUrl);
        resolve(proxyServer);
      });
  });
}

// 业务逻辑
function requestHandle(req: http.IncomingMessage, res: http.ServerResponse) {
  const html = String.raw;
  const content = Buffer.from(html` <!DOCTYPE html>
    <html>
      <head>
        <meta charset="UTF-8" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
      </head>
      <body>
        <h1>hello world</h1>
        <div>
          <h2>URL:</h2>
          <pre>${req.url}</pre>
        </div>
        <div>
          <h2>HEADERS 请求头:</h2>
          <pre>${JSON.stringify(req.headers, null, 2)}</pre>
        </div>
      </body>
    </html>`);
  res.writeHead(200, {
    "content-type": "text/html; charset=utf-8",
    "content-length": content.length,
  });
  res.end(content);
}

// 这里就是入口了
function main(proxyPort: number) {
  return Promise.all([
    createProxyServer(proxyPort),
    createFakeHttpsServer(), //随机端口
  ])
    .then(([proxyServer, fakeServer]) => {
      // 建立客户端到伪服务端的通信隧道
      proxyServer.on(
        "connect",
        onConnectFactory((fakeServer.address() as AddressInfo).port)
      );
      // 伪服务端处理，可以响应自定义内容
      fakeServer.on("request", requestHandle);
    })
    .then(() => {
      console.log("everything is ok");
    });
}

// 监听异常，避免意外退出
process.on("uncaughtException", (err) => {
  console.error(err);
});

main(22600);
