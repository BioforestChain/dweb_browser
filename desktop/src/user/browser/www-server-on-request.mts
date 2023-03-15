import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";

const { IpcResponse, IpcHeaders } = ipc;

export async function wwwServerOnRequest(request: IpcRequest, ipc: Ipc){
    let pathname = request.parsed_url.pathname;
        pathname = pathname === "/" ? "/index.html" : pathname;
        
    const url = `file:///cot-demo${pathname}?mode=stream`
    const response = await jsProcess.nativeRequest(url);

    ipc.postMessage(
      new IpcResponse(
        request.req_id,
        response.statusCode,
        response.headers,
        response.body,
        ipc
      )
    );
}