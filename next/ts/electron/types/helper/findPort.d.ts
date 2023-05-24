/// <reference types="node" />
export declare const isPortInUse: (try_port?: number, server?: import("net").Server) => Promise<number | false>;
export declare const findPort: (favorite_ports?: number[]) => Promise<number>;
