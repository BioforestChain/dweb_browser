import { u8aConcat } from "../../helper/binaryHelper.cjs";
import { simpleEncoder } from "../../helper/encoding.cjs";
import { mapHelper } from "../../helper/mapHelper.cjs";
import { PromiseOut } from "../../helper/PromiseOut.cjs";
import { ReadableStreamOut } from "../../helper/readableStreamHelper.cjs";
import type { ServerUrlInfo } from "../../sys/http-server/const.js";
import { OBSERVE } from "./tool.event.mjs";
import { cros } from "./tool.native.mjs";

const { IpcResponse, Ipc, IpcRequest, IpcHeaders } = ipc;
type $IpcResponse = InstanceType<typeof IpcResponse>;
export type $Ipc = InstanceType<typeof Ipc>;
type $IpcRequest = InstanceType<typeof IpcRequest>;

const ipcObserversMap = new Map<
  $MMID,
  {
    ipc: PromiseOut<$Ipc>;
    obs: Set<{ controller: ReadableStreamDefaultController<Uint8Array> }>;
  }
>();

const INTERNAL_PREFIX = "/internal";
/**
 * request 事件处理器
 */
export async function onApiRequest(
  serverurlInfo: ServerUrlInfo,
  request: $IpcRequest,
  httpServerIpc: $Ipc
) {
  let ipcResponse: undefined | $IpcResponse;
  try {
    const url = new URL(request.url, serverurlInfo.internal_origin);
    if (url.pathname.startsWith(INTERNAL_PREFIX)) {
      const pathname = url.pathname.slice(INTERNAL_PREFIX.length);
      // 转发public url
      if (pathname === "/public-url") {
        ipcResponse = IpcResponse.fromText(
          request.req_id,
          200,
          undefined,
          serverurlInfo.buildPublicUrl(() => { }).href,
          httpServerIpc
        );
        return
      }
      // 监听属性 
      if (pathname === "/observe") {
        const streamPo = observeFactory(url)
        ipcResponse = IpcResponse.fromStream(
          request.req_id,
          200,
          undefined,
          streamPo.stream,
          httpServerIpc
        );
        return
      }
      throw new Error(`unknown gateway: ${url.search}`);
    } else {
      // 转发file请求到目标NMM
      const path = `file:/${url.pathname}${url.search}`;
      console.log("onRequestPath: ", path, request.method, request.body);
      if (request.method === "POST") {
        const response = await jsProcess.nativeFetch(path, {
          body: request.body.raw,
          headers: request.headers,
          method: request.method,
        });

        ipcResponse = await IpcResponse.fromResponse(
          request.req_id,
          response,
          httpServerIpc
          // true
        );
      } else {
        const response = await jsProcess.nativeFetch(path);
        ipcResponse = await IpcResponse.fromResponse(
          request.req_id,
          response,
          httpServerIpc
          // true
        );
      }
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

/**监听属性的变化 */
const observeFactory = (url: URL) => {
  const mmid = url.searchParams.get("mmid");
  console.log("cotDemo#url.mmid=>", mmid)
  if (mmid === null) {
    throw new Error("observe require mmid");
  }
  const streamPo = new ReadableStreamOut<Uint8Array>();
  const observers = mapHelper.getOrPut(ipcObserversMap, mmid, (mmid) => {
    const result = { ipc: new PromiseOut<$Ipc>(), obs: new Set() };
    result.ipc.resolve(jsProcess.connect(mmid));
    result.ipc.promise.then((ipc) => {
      ipc.onEvent((event) => {
        console.log("on-event", event);
        console.log("cotDemo#event.name=>{%s} remote.mmid=>{%s}", event.name, ipc.remote.mmid)
        if (event.name !== OBSERVE.State && event.name !== OBSERVE.UpdateProgress) {
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
  return streamPo
}
