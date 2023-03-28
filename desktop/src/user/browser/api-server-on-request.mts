
// import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
// import { ReadableStreamOut } from "../../helper/readableStreamHelper.cjs"
// import { mapHelper } from "../../helper/mapHelper.cjs"
// import { PromiseOut } from "../../helper/PromiseOut.cjs";
// import { simpleEncoder } from "../../helper/encoding.cjs";
// import { u8aConcat } from "../../helper/binaryHelper.cjs";
import { log } from "../../helper/devtools.cjs";

import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import type { ServerUrlInfo } from "../../sys/http-server/const.js"

const symbolETO = Symbol("***eto***");
const { IpcEvent, IpcResponse } = ipc

export async function createApiServerOnRequest(www_server_internal_origin: string, apiServerUrlInfo: ServerUrlInfo){
    return async (ipcRequest: IpcRequest, ipc: Ipc): Promise<void> => {
        apiServerOnRequest(ipcRequest, ipc, www_server_internal_origin, apiServerUrlInfo)
    }
}

export async function apiServerOnRequest(ipcRequest: IpcRequest, ipc: Ipc, www_server_internal_origin: string, apiServerUrlInfo: ServerUrlInfo){
    const pathname = ipcRequest.parsed_url.pathname;
    console.log('api-server-on-request.mts',ipcRequest.parsed_url)
    
    switch(pathname){
        case pathname.startsWith("/internal") ? pathname : symbolETO:
            apiServerOnRequestInternal(ipcRequest, ipc, www_server_internal_origin, apiServerUrlInfo);
            break;
        default: throw new Error(`[缺少处理器] ${ipcRequest.parsed_url}`);
    }
}

export async function apiServerOnRequestInternal(ipcRequest: IpcRequest, ipc: Ipc, www_server_internal_origin: string, apiServerUrlInfo: ServerUrlInfo){
    const pathname = ipcRequest.parsed_url.pathname;
    switch(pathname){
        case "/internal/public-url":
            apiServerOnRequestInternalPublicUrl(ipcRequest, ipc, www_server_internal_origin, apiServerUrlInfo);
            break;
        default: throw new Error(`[缺少处理器] ${ipcRequest.parsed_url}`);

    }
}

/**
 * 处理 获取 public-url 的请求
 * @param ipcRequest 
 * @param ipc 
 * @param www_server_internal_origin 
 * @param apiServerUrlInfo 
 */
async function  apiServerOnRequestInternalPublicUrl(ipcRequest: IpcRequest, ipc: Ipc, www_server_internal_origin: string, apiServerUrlInfo: ServerUrlInfo){
    const ipcResponse = IpcResponse.fromText(
        ipcRequest.req_id,
        200,
        undefined,
        apiServerUrlInfo.buildPublicUrl(() => { }).href,
        ipc
    );
    ipcResponse.headers.init("Access-Control-Allow-Origin", "*");
    ipcResponse.headers.init("Access-Control-Allow-Headers", "*");  
    ipcResponse.headers.init("Access-Control-Allow-Methods", "*");
    ipc.postMessage(ipcResponse);
}


const { IpcHeaders } = ipc;
export interface $StatusBarState{
    color: string // 十六进制
    insets: {
        bottom: number;
        left: number;
        right: number;
        top: number;
    },
    overlay: boolean;
    style: "DEFAULT" | "DARK" | "LIGHT";
    visible: boolean;
}

 