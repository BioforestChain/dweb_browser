import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import type { Ipc } from "../../core/ipc/ipc.cjs";

/**
 * toast show
 * @param request 
 * @param httpServerIpc 
 */
export async function onRequestToastShow(request: IpcRequest, httpServerIpc: Ipc) {
  const url = `file://toast.sys.dweb${request.url}`;
  console.log("onRequestToastShow: ", url)
  const result = await jsProcess
    .nativeFetch(url)
    .then(async (res: Response) => res)
    .catch((err: any) => {
      console.log("请求失败： ", err)
      return err
    });
  return result
}
