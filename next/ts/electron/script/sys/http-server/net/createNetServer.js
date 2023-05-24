"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.NetServer = exports.PROTOCOLS = exports.http2CreateUnencryptedServer = exports.http2CreateServer = exports.httpCreateServer = exports.httpsCreateServer = void 0;
const https_1 = __importDefault(require("https"));
const httpsCreateServer = async (options, listenOptions) => {
    const { port, hostname = "localhost" } = listenOptions;
    const host = `${hostname}:${port}`;
    const origin = `https://${host}`;
    const server = https_1.default.createServer(options);
    await new Promise((resolve, reject) => {
        server.on("error", reject).listen(port, hostname, resolve);
    });
    return {
        hostname,
        port,
        host,
        origin,
        server,
        protocol: exports.PROTOCOLS.https,
    };
};
exports.httpsCreateServer = httpsCreateServer;
const http_1 = __importDefault(require("http"));
const httpCreateServer = async (options, listenOptions) => {
    const { port, hostname = "localhost" } = listenOptions;
    const host = `${hostname}:${port}`;
    const origin = `http://${host}`;
    const server = http_1.default.createServer(options);
    await new Promise((resolve, reject) => {
        server.on("error", reject).listen(port, hostname, resolve);
    });
    return {
        hostname,
        port,
        host,
        origin,
        server,
        protocol: exports.PROTOCOLS.http1,
    };
};
exports.httpCreateServer = httpCreateServer;
const http2_1 = __importDefault(require("http2"));
const http2CreateServer = async (options, listenOptions) => {
    const { port, hostname = "localhost" } = listenOptions;
    const host = `${hostname}:${port}`;
    const origin = `https://${host}`;
    const server = http2_1.default.createSecureServer(options);
    await new Promise((resolve, reject) => {
        server.on("error", reject).listen(port, hostname, resolve);
    });
    return {
        hostname,
        port,
        host,
        origin,
        server,
        protocol: exports.PROTOCOLS.http2,
    };
};
exports.http2CreateServer = http2CreateServer;
const http2CreateUnencryptedServer = async (options, listenOptions) => {
    const { port, hostname = "localhost" } = listenOptions;
    const host = `${hostname}:${port}`;
    const origin = `http://${host}`;
    const server = http2_1.default.createServer(options);
    await new Promise((resolve, reject) => {
        server.on("error", reject).listen(port, hostname, resolve);
    });
    return {
        hostname,
        port,
        host,
        origin,
        server,
        protocol: exports.PROTOCOLS.http1,
    };
};
exports.http2CreateUnencryptedServer = http2CreateUnencryptedServer;
exports.PROTOCOLS = {
    http2: { prefix: "https://", protocol: "https:", port: 443 },
    http2_unencrypted: { prefix: "http://", protocol: "http:", port: 80 },
    https: { prefix: "https://", protocol: "https:", port: 443 },
    http1: { prefix: "http://", protocol: "http:", port: 80 },
};
class NetServer {
}
exports.NetServer = NetServer;
