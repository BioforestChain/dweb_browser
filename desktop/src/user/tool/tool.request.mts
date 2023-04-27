import { IpcEvent } from "../../core/ipc/IpcEvent.cjs";
import { u8aConcat } from "../../helper/binaryHelper.cjs";
import { createSignal } from "../../helper/createSignal.cjs";
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
type $OnIpcRequestUrl = (request: $IpcRequest, url: URL) => void
const fetchSignal = createSignal<$OnIpcRequestUrl>()


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
    // 是否是内部请求
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
      }
    } else {
      fetchSignal.emit(request, url)

      // 转发file请求到目标NMM
      const path = `file:/${url.pathname}${url.search}`;
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

    }
    if (!ipcResponse) {
      throw new Error(`unknown gateway: ${url.search}`);
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

const serviceWorkerFetch = () => {
  const result = { ipc: new PromiseOut<$Ipc>() };
  result.ipc.resolve(jsProcess.connect("mwebview.sys.dweb"))
  result.ipc.promise.then((ipc) => {
    fetchSignal.listen((request, url) => {
      ipc.postMessage(IpcEvent.fromText("fetch", ""))
    })
  })
}
// serviceWorker fetch
serviceWorkerFetch()


/**监听属性的变化 */
const observeFactory = (url: URL) => {
  const mmid = url.searchParams.get("mmid");
  if (mmid === null) {
    throw new Error("observe require mmid");
  }
  const streamPo = new ReadableStreamOut<Uint8Array>();
  const observers = mapHelper.getOrPut(ipcObserversMap, mmid, (mmid) => {
    const result = { ipc: new PromiseOut<$Ipc>(), obs: new Set() };
    result.ipc.resolve(jsProcess.connect(mmid));
    result.ipc.promise.then((ipc) => {
      ipc.onEvent((event) => {
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
