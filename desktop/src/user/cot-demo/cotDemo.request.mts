import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";

/**
 * toast show
 * @param request 
 * @param httpServerIpc 
 */
export function onRequestToastShow(request: IpcRequest, httpServerIpc: Ipc) {
  const url = `file://toast.sys.dweb${request.url}`;
  console.log("onRequestToastShow: ", url)
  jsProcess
    .nativeFetch(url)
    .then(async (res: Response) => {
      // 这里要做修改 改为 IpcResponse.fromResponse
      httpServerIpc.postMessage(
        await IpcResponse.fromResponse(request.req_id, res, httpServerIpc)
      );
    })
    .catch((err: any) => console.log("请求失败： ", err));
}
