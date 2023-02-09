"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const node_forge_1 = __importDefault(require("node-forge"));
const node_fs_1 = __importDefault(require("node:fs"));
const node_http_1 = __importDefault(require("node:http"));
const node_https_1 = __importDefault(require("node:https"));
const node_net_1 = __importDefault(require("node:net"));
const node_tls_1 = __importDefault(require("node:tls"));
const node_url_1 = __importDefault(require("node:url"));
const cert_cjs_1 = require("./cert.cjs");
node_http_1.default && node_https_1.default && node_fs_1.default && node_net_1.default && node_tls_1.default && node_url_1.default && node_forge_1.default && cert_cjs_1.createServerCertificate;
function onConnectFactory(targetServerPort) {
    return function connect(clientRequest, clientSocket, head) {
        // 连接目标服务器
        const targetSocket = node_net_1.default.connect(targetServerPort, "127.0.0.1", () => {
            // 通知客户端已经建立连接
            clientSocket.write("HTTP/1.1 200 Connection Established\r\n" +
                "Proxy-agent: MITM-proxy\r\n" +
                "\r\n");
            // 建立通信隧道，转发数据
            targetSocket.write(head);
            clientSocket.pipe(targetSocket).pipe(clientSocket);
        });
    };
}
/** 创建支持多域名的 https 服务 **/
function createFakeHttpsServer(fakeServerPort = 0) {
    return new Promise((resolve, reject) => {
        const fakeServer = new node_https_1.default.Server({
            SNICallback: (hostname, callback) => {
                const { key, cert } = (0, cert_cjs_1.createServerCertificate)(hostname);
                callback(null, node_tls_1.default.createSecureContext({
                    key: node_forge_1.default.pki.privateKeyToPem(key),
                    cert: node_forge_1.default.pki.certificateToPem(cert),
                }));
            },
        });
        fakeServer.on("error", reject).listen(fakeServerPort, () => {
            resolve(fakeServer);
        });
    });
}
function createProxyServer(proxyPort) {
    return new Promise((resolve, reject) => {
        const serverCrt = (0, cert_cjs_1.createServerCertificate)("localhost");
        const proxyServer = node_https_1.default
            .createServer({
            key: node_forge_1.default.pki.privateKeyToPem(serverCrt.key),
            cert: node_forge_1.default.pki.certificateToPem(serverCrt.cert),
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
function requestHandle(req, res) {
    const html = String.raw;
    const content = Buffer.from(html ` <!DOCTYPE html>
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
        "Content-Type": "text/html; charset=utf-8",
        "Content-Length": content.length,
    });
    res.end(content);
}
// 这里就是入口了
function main(proxyPort) {
    return Promise.all([
        createProxyServer(proxyPort),
        createFakeHttpsServer(), //随机端口
    ])
        .then(([proxyServer, fakeServer]) => {
        // 建立客户端到伪服务端的通信隧道
        proxyServer.on("connect", onConnectFactory(fakeServer.address().port));
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
