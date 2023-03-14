import type { Ipc } from "../../core/ipc/ipc.cjs";
import type { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import type { IpcRequest } from "../../core/ipc/IpcRequest.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import type { ServerUrlInfo } from "../../sys/http-server/const.js";

const INTERNAL_PREFIX = "/internal";
/**
 * request 事件处理器
 */
export async function onApiRequest(
  serverurlInfo: ServerUrlInfo,
  request: IpcRequest,
  httpServerIpc: Ipc
) {
  let ipcResponse: undefined | IpcResponse;
  try {
    const url = new URL(request.url, serverurlInfo.internal_origin);
    if (url.pathname.startsWith(INTERNAL_PREFIX)) {
      const pathname = url.pathname.slice(INTERNAL_PREFIX.length);
      if (pathname === "/public-url") {
        ipcResponse = await IpcResponse.fromText(
          request.req_id,
          200,
          undefined,
          serverurlInfo.buildPublicUrl(() => {}).href,
          httpServerIpc
        );
      } else {
        throw new Error(`unknown gateway: ${url.search}`);
      }
    } else {
      const path = `file:/${url.pathname}${url.search}`;
      console.log("onRequestToastShow: ", path);
      let res = await jsProcess.nativeFetch(path);

      ipcResponse = await IpcResponse.fromResponse(
        request.req_id,
        res,
        httpServerIpc
      );
    }
    cros(ipcResponse.headers);
    // 返回数据到前端
    httpServerIpc.postMessage(ipcResponse);
  } catch (err) {
    if (ipcResponse === undefined) {
      ipcResponse = await IpcResponse.fromText(
        request.req_id,
        502,
        undefined,
        String(err),
        httpServerIpc
      );
      cros(ipcResponse.headers);
      httpServerIpc.postMessage(ipcResponse);
    } else {
      throw err;
    }
  }
}

const cros = (headers: IpcHeaders) => {
  headers.init("Access-Control-Allow-Origin", "*");
  headers.init("Access-Control-Allow-Headers", "*"); // 要支持 X-Dweb-Host
  headers.init("Access-Control-Allow-Methods", "*");
};
