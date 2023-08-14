import http from "node:http";
import https from "node:https";
import { $Protocol, $ServerInfo } from "./types.ts";
export * from "./types.ts";

export type $HttpsServerInfo = $ServerInfo<https.Server>;
export const httpsCreateServer = async (
  options: https.ServerOptions,
  listenOptions: { port: number; hostname?: string }
) => {
  const { port, hostname = "localhost" } = listenOptions;
  const host = `${hostname}:${port}`;
  const origin = `https://${host}`;

  const server = https.createServer(options);

  await new Promise<void>((resolve, reject) => {
    server.on("error", reject).listen(port, hostname, resolve);
  });

  return {
    hostname,
    port,
    host,
    origin,
    server,
    protocol: PROTOCOLS.https,
  } satisfies $HttpsServerInfo;
};

export type $HttpServerInfo = $ServerInfo<http.Server>;
export const httpCreateServer = async (
  options: http.ServerOptions,
  listenOptions: { port: number; hostname?: string }
) => {
  const { port, hostname = "localhost" } = listenOptions;
  const host = `${hostname}:${port}`;
  const origin = `http://${host}`;
  const server = http.createServer(options);
  await new Promise<void>((resolve, reject) => {
    server.on("error", reject).listen(port, hostname, resolve);
  });

  return {
    hostname,
    port,
    host,
    origin,
    server,
    protocol: PROTOCOLS.http1,
  } satisfies $HttpServerInfo;
};

import http2 from "node:http2";
export type $Http2ServerInfo = $ServerInfo<http2.Http2SecureServer>;
export const http2CreateServer = async (
  options: http2.SecureServerOptions,
  listenOptions: { port: number; hostname?: string }
) => {
  const { port, hostname = "localhost" } = listenOptions;
  const host = `${hostname}:${port}`;
  const origin = `https://${host}`;

  const server = http2.createSecureServer(options);
  await new Promise<void>((resolve, reject) => {
    server.on("error", reject).listen(port, hostname, resolve);
  });

  return {
    hostname,
    port,
    host,
    origin,
    server,
    protocol: PROTOCOLS.http2,
  } satisfies $Http2ServerInfo;
};
export type $Http2UnencryptedServerInfo = $ServerInfo<http2.Http2Server>;
export const http2CreateUnencryptedServer = async (
  options: http2.ServerOptions,
  listenOptions: { port: number; hostname?: string }
) => {
  const { port, hostname = "localhost" } = listenOptions;
  const host = `${hostname}:${port}`;
  const origin = `http://${host}`;

  const server = http2.createServer(options);
  await new Promise<void>((resolve, reject) => {
    server.on("error", reject).listen(port, hostname, resolve);
  });

  return {
    hostname,
    port,
    host,
    origin,
    server,
    protocol: PROTOCOLS.http1,
  } satisfies $Http2UnencryptedServerInfo;
};

export const PROTOCOLS = {
  http2: { prefix: "https://", protocol: "https:", port: 443 },
  http2_unencrypted: { prefix: "http://", protocol: "http:", port: 80 },
  https: { prefix: "https://", protocol: "https:", port: 443 },
  http1: { prefix: "http://", protocol: "http:", port: 80 },
} satisfies Record<string, $Protocol>;

// deno-lint-ignore no-explicit-any
export abstract class NetServer<S extends $ServerInfo<any>> {
  abstract create(): Promise<S>;
  abstract destroy(): unknown;
  abstract readonly info: S | undefined;
}
