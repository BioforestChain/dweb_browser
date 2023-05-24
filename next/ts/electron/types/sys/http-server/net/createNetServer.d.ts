/// <reference types="node" />
/// <reference types="node" />
/// <reference types="node" />
import https from "https";
export declare const httpsCreateServer: (options: https.ServerOptions, listenOptions: {
    port: number;
    hostname?: string;
}) => Promise<{
    hostname: string;
    port: number;
    host: string;
    origin: string;
    server: https.Server<typeof http.IncomingMessage, typeof http.ServerResponse>;
    protocol: {
        prefix: string;
        protocol: string;
        port: number;
    };
}>;
export type $HttpsServerInfo = $ServerInfo<https.Server>;
import http from "http";
export declare const httpCreateServer: (options: http.ServerOptions, listenOptions: {
    port: number;
    hostname?: string;
}) => Promise<{
    hostname: string;
    port: number;
    host: string;
    origin: string;
    server: http.Server<typeof http.IncomingMessage, typeof http.ServerResponse>;
    protocol: {
        prefix: string;
        protocol: string;
        port: number;
    };
}>;
export type $HttpServerInfo = $ServerInfo<http.Server>;
import http2 from "http2";
export declare const http2CreateServer: (options: http2.SecureServerOptions, listenOptions: {
    port: number;
    hostname?: string;
}) => Promise<{
    hostname: string;
    port: number;
    host: string;
    origin: string;
    server: http2.Http2SecureServer;
    protocol: {
        prefix: string;
        protocol: string;
        port: number;
    };
}>;
export type $Http2ServerInfo = $ServerInfo<http2.Http2SecureServer>;
export declare const http2CreateUnencryptedServer: (options: http2.ServerOptions, listenOptions: {
    port: number;
    hostname?: string;
}) => Promise<{
    hostname: string;
    port: number;
    host: string;
    origin: string;
    server: http2.Http2Server;
    protocol: {
        prefix: string;
        protocol: string;
        port: number;
    };
}>;
export type $Http2UnencryptedServerInfo = $ServerInfo<http2.Http2Server>;
export declare const PROTOCOLS: {
    http2: {
        prefix: string;
        protocol: string;
        port: number;
    };
    http2_unencrypted: {
        prefix: string;
        protocol: string;
        port: number;
    };
    https: {
        prefix: string;
        protocol: string;
        port: number;
    };
    http1: {
        prefix: string;
        protocol: string;
        port: number;
    };
};
export interface $Protocol {
    protocol: string;
    prefix: string;
    port: number;
}
export interface $NetServer<S extends $ServerInfo<any>> {
    create(): Promise<S>;
    destroy(): unknown;
}
export declare abstract class NetServer<S extends $ServerInfo<any>> implements $NetServer<S> {
    abstract create(): Promise<S>;
    abstract destroy(): unknown;
    abstract readonly info: S | undefined;
}
export interface $DwebHttpServerOptions {
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
