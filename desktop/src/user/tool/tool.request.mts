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
type $OnIpcRequestUrl = (request: $IpcRequest) => void
const fetchSignal = createSignal<$OnIpcRequestUrl>()
export const onFetchSignal = createSignal<$OnIpcRequestUrl>()
// serviceWorker 的fetch锁，如果打开了我们就不帮忙处理请求，让前端自己处理
let fetchLock = false;
const fetchSet = new Map<string, number>();

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
      ipcResponse = internalFactory(url, request.req_id, httpServerIpc, serverurlInfo)
    } else {
      // 如果用户要自己处理
      if (fetchLock && (request.method === "GET" || request.method === "HEAD")) {
        // 已经转发到serviceWorker去了，那么就不要再抛给用户了
        if (!fetchSet.has(url.pathname)) {
          fetchSet.set(url.pathname, request.req_id)
          // 触发fetch
          return fetchSignal.emit(request)
        }
      }

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
      );
      //  如果请求被用户拦截过
      if (fetchSet.has(url.pathname)) {
        const req_id = fetchSet.get(url.pathname);
        if (!req_id) return
        const ipcResponse = await IpcResponse.fromResponse(
          req_id,
          response,
          httpServerIpc
        );
        cros(ipcResponse.headers);
        // 返回数据还给拦截之前的请求
        httpServerIpc.postMessage(ipcResponse);
        fetchSet.delete(url.pathname)
      }

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

/**处理内部的绑定流事件 */
const internalFactory = (url: URL, req_id: number, httpServerIpc: $Ipc, serverurlInfo: ServerUrlInfo) => {
  const pathname = url.pathname.slice(INTERNAL_PREFIX.length);
  // 转发public url
  if (pathname === "/public-url") {
    return IpcResponse.fromText(
      req_id,
      200,
      undefined,
      serverurlInfo.buildPublicUrl(() => { }).href,
      httpServerIpc
    );
  }
  // 监听属性 
  if (pathname === "/observe") {
    const mmid = url.searchParams.get("mmid");
    if (mmid === null) {
      throw new Error("observe require mmid");
    }
    const streamPo = observeFactory(mmid)
    return IpcResponse.fromStream(
      req_id,
      200,
      undefined,
      streamPo.stream,
      httpServerIpc
    );
  }
  // 监听fetch
  if (pathname === "/fetch") {
    fetchLock = true
    // serviceWorker fetch
    const streamPo = serviceWorkerFetch()
    return IpcResponse.fromStream(
      req_id,
      200,
      undefined,
      streamPo.stream,
      httpServerIpc
    );
  }

  // 监听Onfetch
  if (pathname === "/onFetch") {
    // serviceWorker fetch
    const streamPo = serviceWorkerOnFetch()
    return IpcResponse.fromStream(
      req_id,
      200,
      undefined,
      streamPo.stream,
      httpServerIpc
    );
  }
}

/**这里会处理api的消息返回到前端serviceWorker 构建onFetchEvent 并触发fetch事件 */
const serviceWorkerFetch = () => {
  const streamPo = new ReadableStreamOut<Uint8Array>();
  const ob = { controller: streamPo.controller };
  fetchSignal.listen((ipcRequest) => {
    const jsonlineEnd = simpleEncoder("\n", "utf8");
    const json = ipcRequest.toJSON()
    const uint8 = simpleEncoder(JSON.stringify(json), "utf8")
    ob.controller.enqueue(u8aConcat([uint8, jsonlineEnd]));
  })
  return streamPo
}

/**这里会处理别人发给这个app的消息 */
const serviceWorkerOnFetch = () => {
  const streamPo = new ReadableStreamOut<Uint8Array>();
  const ob = { controller: streamPo.controller };
  onFetchSignal.listen((ipcRequest) => {
    const jsonlineEnd = simpleEncoder("\n", "utf8");
    const json = ipcRequest.toJSON()
    const uint8 = simpleEncoder(JSON.stringify(json), "utf8")
    ob.controller.enqueue(u8aConcat([uint8, jsonlineEnd]));
  })
  return streamPo
}



/**监听属性的变化 */
const observeFactory = (mmid: string) => {
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
