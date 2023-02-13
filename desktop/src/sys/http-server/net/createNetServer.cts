import https from "node:https";
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
export type $HttpsServerInfo = $ServerInfo<https.Server>;

import http from "node:http";
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
export type $HttpServerInfo = $ServerInfo<http.Server>;

import http2 from "node:http2";
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
export type $Http2ServerInfo = $ServerInfo<http2.Http2SecureServer>;
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
export type $Http2UnencryptedServerInfo = $ServerInfo<http2.Http2Server>;

export const PROTOCOLS = {
  http2: { prefix: "https://", protocol: "https:", port: 443 },
  http2_unencrypted: { prefix: "http://", protocol: "http:", port: 80 },
  https: { prefix: "https://", protocol: "https:", port: 443 },
  http1: { prefix: "http://", protocol: "http:", port: 80 },
} satisfies Record<string, $Protocol>;

export interface $Protocol {
  protocol: string;
  prefix: string;
  port: number;
}

export interface $NetServer<S extends $ServerInfo<any>> {
  create(): Promise<S>;
  destroy(): unknown;
  getHost(options: $GetHostOptions): { host: string; origin: string };
}
export abstract class NetServer<S extends $ServerInfo<any>>
  implements $NetServer<S>
{
  abstract create(): Promise<S>;
  abstract destroy(): unknown;
  abstract readonly info: S | undefined;
  getHost(options: $GetHostOptions) {
    const info = this.info;
    if (info === undefined) {
      throw new Error("no created server info");
    }
    const { mmid } = options.ipc.remote;
    const { port = info.protocol.port } = options;
    let subdomain = options.subdomain?.trim() ?? "";
    if (subdomain.length > 0 && subdomain.endsWith(".") === false) {
      subdomain = subdomain + ".";
    }
    const host = this._getHost(subdomain, mmid, port, info);
    const origin = `${info.protocol.prefix}${host}`;
    return {
      origin,
      host,
    };
  }
  abstract _getHost(
    subdomain: string,
    mmid: string,
    port: number,
    info: S
  ): string;
}

import type { Ipc } from "../../../core/ipc/ipc.cjs";
export interface $GetHostOptions {
  ipc: Ipc;
  port?: number;
  subdomain?: string;
}

export interface $ServerInfo<S> {
  server: S;

  host: string;
  hostname: string;
  port: number;
  origin: string;
  protocol: $Protocol;
}
