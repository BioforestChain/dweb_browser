/// <reference types="node" />
import type { OutgoingMessage } from "http";
import type { $BootstrapContext } from "../../core/bootstrapContext.js";
import { NativeMicroModule } from "../../core/micro-module.native.js";
import type { HttpDwebServer } from "../http-server/$createHttpDwebServer.js";
export declare class JmmNMM extends NativeMicroModule {
    mmid: "jmm.sys.dweb";
    downloadStatus: DOWNLOAD_STATUS;
    wwwServer: HttpDwebServer | undefined;
    apiServer: HttpDwebServer | undefined;
    donwloadStramController: ReadableStreamDefaultController | undefined;
    downloadStream: ReadableStream | undefined;
    resume: {
        handler: Function;
        response: OutgoingMessage | undefined;
    };
    _bootstrap(context: $BootstrapContext): Promise<void>;
    protected _shutdown(): unknown;
    private openInstallPage;
}
export interface $State {
    percent: number;
    speed: number;
    size: {
        total: number;
        transferred: number;
    };
    time: {
        elapsed: number;
        remaining: number;
    };
}
export interface $AppMetaData {
    title: string;
    subtitle: string;
    id: string;
    downloadUrl: string;
    icon: string;
    images: string[];
    introduction: string;
    author: string[];
    version: string;
    keywords: string[];
    home: string;
    mainUrl: string;
    server: {
        root: string;
        entry: string;
    };
    splashScreen: {
        entry: string;
    };
    staticWebServers: $StaticWebServers[];
    openWebViewList: [];
    size: string;
    fileHash: "";
    permissions: string[];
    plugins: string[];
    releaseDate: string[];
}
export interface $StaticWebServers {
    root: string;
    entry: string;
    subdomain: string;
    port: number;
}
export declare enum DOWNLOAD_STATUS {
    DOWNLOAD = 0,
    PAUSE = 1,
    CANCEL = 2
}
