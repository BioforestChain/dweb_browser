
// import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { ReadableStreamOut } from "../../helper/readableStreamHelper.cjs"
import { mapHelper } from "../../helper/mapHelper.cjs"
import { PromiseOut } from "../../helper/PromiseOut.cjs";
import { simpleEncoder } from "../../helper/encoding.cjs";
import { u8aConcat } from "../../helper/binaryHelper.cjs";
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
        case pathname.startsWith("/status-bar.sys.dweb") ? pathname : symbolETO:
            apiServerOnRequestStatusbar(ipcRequest, ipc, pathname, www_server_internal_origin);
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
        case "/internal/observe":
            apiServerOnRequestInternalObserver(ipcRequest, ipc, www_server_internal_origin, apiServerUrlInfo);
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

function apiServerOnRequestInternalObserver(ipcRequest: IpcRequest, ipc: Ipc, www_server_internal_origin: string, apiServerUrlInfo: ServerUrlInfo){
    console.error('apiServerOnRequestInternalObserver')
    const url = new URL(ipcRequest.url, apiServerUrlInfo.internal_origin);
    const mmid = url.searchParams.get("mmid");
    if (mmid === null) {
      throw new Error("observe require mmid");
    }
    const streamPo = new ReadableStreamOut<Uint8Array>();
    const observers = mapHelper.getOrPut(ipcObserversMap, mmid, (mmid) => {
        const result = { ipc: new PromiseOut<$Ipc>(), obs: new Set() };
        // 这里有一个问题

        result.ipc.resolve(jsProcess.connect(mmid));
        result.ipc.promise.then((ipc) => {
            console.error('connect之后 ipc.remote.mmid: ', ipc.remote.mmid)
            // 一个 window 对象有多个 app 被打开的情况下，
            // status-bar.sys.dweb 无法判断当前 APP 所匹配的 status-bar
            // 之前是根据 url 判断的 
            // 所以需要报 app_url 发送给 status-bar.sys.dweb 
            ipc.postMessage(
                IpcEvent.fromText(
                    'send-url',
                    www_server_internal_origin
                )
            )

            ipc.onEvent((event) => {
                console.log("on-event", event);
                if (event.name !== "observe") {
                    return;
                }
                const observers = ipcObserversMap.get(ipc.remote.mmid);
                const jsonlineEnd = simpleEncoder("\n", "utf8");
                if (observers && observers.obs.size > 0) {
                    for (const ob of observers.obs) {
                    ob.controller.enqueue(u8aConcat([event.binary, jsonlineEnd]));
                    }
                }
            });
      });
      return result;
    });
    const ob = { controller: streamPo.controller };
    observers.obs.add(ob);
    streamPo.onCancel(() => {
      observers.obs.delete(ob);
    });

    const ipcResponse = IpcResponse.fromStream(
      ipcRequest.req_id,
      200,
      undefined,
      streamPo.stream,
      ipc
    );
    
    ipcResponse.headers.init("Access-Control-Allow-Origin", "*");
    ipcResponse.headers.init("Access-Control-Allow-Headers", "*");  
    ipcResponse.headers.init("Access-Control-Allow-Methods", "*");
    ipc.postMessage(ipcResponse);
}


async function apiServerOnRequestStatusbar(ipcRequest: IpcRequest, ipc: Ipc, pathname: string, internal_origin: string){
    switch(pathname){
        case pathname.endsWith("setBackgroundColor") ? pathname : symbolETO:
            statusbarSetBackgroundColor(ipcRequest, ipc, internal_origin);
            break;
        case pathname.endsWith('/getInfo') ? pathname : symbolETO:
            apiServerGetBackgroundColor(ipcRequest, ipc, internal_origin);
            break;
        case pathname.endsWith(`/setStyle`) ? pathname : symbolETO:
            apiServerSetStyle(ipcRequest, ipc, internal_origin);
            break;
        default: log.red(`缺少 statusbar-bar.sys.dweb 处理器 ${ipcRequest.parsed_url} pathname === ${pathname}`)
    }
}



/**
 * 设置状态栏的背景色
 * @param ipcRequest 
 * @param ipc 
 */
 async function statusbarSetBackgroundColor(ipcRequest: IpcRequest, ipc: Ipc, internal_origin: string){
    const response = await jsProcess.nativeFetch(`file://status-bar.sys.dweb/operation/set_background_color${ipcRequest.parsed_url.search}&app_url=${internal_origin}`)
    ipc.postMessage(
        await IpcResponse.fromResponse(
            ipcRequest.req_id,
            response,
            ipc,
        )
    );
}

/**
 * 获取状态栏颜色
 * @param ipcRequest 
 * @param ipc 
 * @param internal_origin 
 */
async function apiServerGetBackgroundColor(ipcRequest: IpcRequest, ipc: Ipc, internal_origin: string){
    const response = await jsProcess.nativeFetch(`file://status-bar.sys.dweb/operation/get_background_color?app_url=${internal_origin}`)
    ipc.postMessage(
        await IpcResponse.fromResponse(
            ipcRequest.req_id,
            response,
            ipc,
        )
    );
}

/**
 * 设置状态栏的style
 * @param ipcRequest 
 * @param ipc 
 * @param internal_origin 
 */
async function apiServerSetStyle(ipcRequest: IpcRequest, ipc: Ipc, internal_origin: string){
    log.red(`api-server-on-request.mts ipcRequest.parsed_url.search==${ipcRequest.parsed_url.search}`)
    const response = await jsProcess.nativeFetch(`file://status-bar.sys.dweb/operation/set_style${ipcRequest.parsed_url.search}&app_url=${internal_origin}`)
    ipc.postMessage(
        await IpcResponse.fromResponse(
            ipcRequest.req_id,
            response,
            ipc,
        )
    );
}

const { IpcHeaders } = ipc;
const ipcObserversMap = new Map<
  $MMID,
  {
    ipc: PromiseOut<$Ipc>;
    obs: Set<{ controller: ReadableStreamDefaultController<Uint8Array> }>;
  }
>();
type $IpcResponse = InstanceType<typeof IpcResponse>;
type $Ipc = InstanceType<typeof Ipc>;
type $IpcRequest = InstanceType<typeof IpcRequest>;
type $IpcHeaders = InstanceType<typeof IpcHeaders>;

 