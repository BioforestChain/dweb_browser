import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";

const symbolETO = Symbol("***eto***");

export async function apiServerOnRequest(ipcRequest: IpcRequest, ipc: Ipc){
    console.log("[api-server-on-rquest.mts] ipcReqeust: ", ipcRequest)
    const pathname = ipcRequest.parsed_url.pathname;
    switch(pathname){
        case pathname.startsWith("/status-bar.sys.dweb") ? pathname : symbolETO:
            apiServerOnRequestStatusbar(ipcRequest, ipc, pathname);
            break;
    }
}


async function apiServerOnRequestStatusbar(ipcRequest: IpcRequest, ipc: Ipc, pathname: string){
 
    switch(pathname){
        case pathname.endsWith("setBackgroundColor") ? pathname : symbolETO:
            statusbarSetBackgroundColor(ipcRequest, ipc);
            break;
    }
}



/**
 * 这只状态栏的背景色
 * @param ipcRequest 
 * @param ipc 
 */
async function statusbarSetBackgroundColor(ipcRequest: IpcRequest, ipc: Ipc){
    
}

 