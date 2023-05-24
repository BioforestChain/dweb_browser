/// <reference types="node" />
import { $HttpServerInfo, NetServer } from "./createNetServer.js";
/**
 * 类似 https.createServer
 * 差别在于服务只在本地运作
 */
export declare class Http1Server extends NetServer<$HttpServerInfo> {
    static readonly PREFIX = "http://";
    static readonly PROTOCOL = "http:";
    static readonly PORT = 80;
    private _info?;
    get info(): $HttpServerInfo | undefined;
    private bindingPort;
    get authority(): string;
    get origin(): string;
    create(): Promise<{
        hostname: string;
        port: number;
        host: string;
        origin: string;
        server: import("http").Server<typeof import("http").IncomingMessage, typeof import("http").ServerResponse>;
        protocol: {
            prefix: string;
            protocol: string;
            port: number;
        };
    }>;
    destroy(): void;
}
