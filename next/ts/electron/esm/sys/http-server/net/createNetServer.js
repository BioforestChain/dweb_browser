import https from "https";
export const httpsCreateServer = async (options, listenOptions) => {
    const { port, hostname = "localhost" } = listenOptions;
    const host = `${hostname}:${port}`;
    const origin = `https://${host}`;
    const server = https.createServer(options);
    await new Promise((resolve, reject) => {
        server.on("error", reject).listen(port, hostname, resolve);
    });
    return {
        hostname,
        port,
        host,
        origin,
        server,
        protocol: PROTOCOLS.https,
    };
};
import http from "http";
export const httpCreateServer = async (options, listenOptions) => {
    const { port, hostname = "localhost" } = listenOptions;
    const host = `${hostname}:${port}`;
    const origin = `http://${host}`;
    const server = http.createServer(options);
    await new Promise((resolve, reject) => {
        server.on("error", reject).listen(port, hostname, resolve);
    });
    return {
        hostname,
        port,
        host,
        origin,
        server,
        protocol: PROTOCOLS.http1,
    };
};
import http2 from "http2";
export const http2CreateServer = async (options, listenOptions) => {
    const { port, hostname = "localhost" } = listenOptions;
    const host = `${hostname}:${port}`;
    const origin = `https://${host}`;
    const server = http2.createSecureServer(options);
    await new Promise((resolve, reject) => {
        server.on("error", reject).listen(port, hostname, resolve);
    });
    return {
        hostname,
        port,
        host,
        origin,
        server,
        protocol: PROTOCOLS.http2,
    };
};
export const http2CreateUnencryptedServer = async (options, listenOptions) => {
    const { port, hostname = "localhost" } = listenOptions;
    const host = `${hostname}:${port}`;
    const origin = `http://${host}`;
    const server = http2.createServer(options);
    await new Promise((resolve, reject) => {
        server.on("error", reject).listen(port, hostname, resolve);
    });
    return {
        hostname,
        port,
        host,
        origin,
        server,
        protocol: PROTOCOLS.http1,
    };
};
export const PROTOCOLS = {
    http2: { prefix: "https://", protocol: "https:", port: 443 },
    http2_unencrypted: { prefix: "http://", protocol: "http:", port: 80 },
    https: { prefix: "https://", protocol: "https:", port: 443 },
    http1: { prefix: "http://", protocol: "http:", port: 80 },
};
export class NetServer {
}
