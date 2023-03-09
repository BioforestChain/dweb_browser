import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";


/**
* request 事件处理器
*/
export async function onApiRequest(request: IpcRequest, httpServerIpc: Ipc) {
  const url = `file:/${request.url}`;
  console.log("onRequestToastShow: ", url)
  let res = await jsProcess.nativeFetch(url)
  // 返回数据到前端
  httpServerIpc.postMessage(
    await IpcResponse.fromText(
      request.req_id,
      200,
      new IpcHeaders({
        "content-type": "text/html",
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Headers": "*", // 要支持 X-Dweb-Host
        "Access-Control-Allow-Methods": "*",
      }),
      await res.text(),
      httpServerIpc
    )
  );
}

