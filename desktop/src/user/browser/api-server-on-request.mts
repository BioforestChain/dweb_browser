import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";

const symbolETO = Symbol("***eto***");


export async function createApiServerOnRequest(internal_origin: string){
    return async (ipcRequest: IpcRequest, ipc: Ipc): Promise<void> => {
        apiServerOnRequest(ipcRequest, ipc, internal_origin)


    }
}

export async function apiServerOnRequest(ipcRequest: IpcRequest, ipc: Ipc, internal_origin: string){
    console.log("[api-server-on-rquest.mts] ipcReqeust: ", ipcRequest)
    const pathname = ipcRequest.parsed_url.pathname;
    switch(pathname){
        case pathname.startsWith("/status-bar.sys.dweb") ? pathname : symbolETO:
            apiServerOnRequestStatusbar(ipcRequest, ipc, pathname, internal_origin);
            break;
    }
}


async function apiServerOnRequestStatusbar(ipcRequest: IpcRequest, ipc: Ipc, pathname: string, internal_origin: string){
 
    switch(pathname){
        case pathname.endsWith("setBackgroundColor") ? pathname : symbolETO:
            statusbarSetBackgroundColor(ipcRequest, ipc, internal_origin);
            break;
    }
}



/**
 * 这只状态栏的背景色
 * @param ipcRequest 
 * @param ipc 
 */
async function statusbarSetBackgroundColor(ipcRequest: IpcRequest, ipc: Ipc, internal_origin: string){
    const href = ipcRequest.parsed_url.href
    const response = await jsProcess.nativeFetch(`file://statusbar.sys.dweb/operation${ipcRequest.parsed_url.search}&app_url=${internal_origin}`)
    ipc.postMessage(
        await IpcResponse.fromResponse(
            ipcRequest.req_id,
            response,
            ipc,
        )
    );
}

 