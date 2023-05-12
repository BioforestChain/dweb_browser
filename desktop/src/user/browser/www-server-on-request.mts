import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";

const { IpcResponse, IpcHeaders } = ipc;

export async function wwwServerOnRequest(request: IpcRequest, ipc: Ipc){

  let pathname = request.parsed_url.pathname;
      pathname = pathname === "/" ? "/index.html" : pathname;
  
  // 打开 dweb_browser/example/vue3 这个 demo 的路径 
  const url = `file:///app/cot-demo${pathname}?mode=stream`
  // 打开首页的 路径
  // const url = `file:///assets/html/browser.html?mode=stream`;
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