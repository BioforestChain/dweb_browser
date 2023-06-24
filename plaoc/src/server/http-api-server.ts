import {
  $Ipc,
  $IpcRequest,
  $IpcResponse,
  $ServerUrlInfo,
  HttpDwebServer,
  IpcRequest,
  IpcResponse,
  IPC_METHOD,
  jsProcess,
} from "./deps.ts";
import { cros, HttpServer } from "./http-helper.ts";

/**给前端的api服务 */
export class Server_api extends HttpServer {
  protected _getOptions(): $DwebHttpServerOptions {
    return {
      subdomain: "api",
      port: 443,
    };
  }
  async start(wwwServer: HttpDwebServer, externalServer: HttpDwebServer) {
    const apiServer = await this._serverP;
    const apiReadableStreamIpc = await apiServer.listen();

    apiReadableStreamIpc.onRequest(async (request, ipc) => {
      if (request.method === IPC_METHOD.OPTIONS) {
        return ipc.postMessage(
          IpcResponse.fromText(
            request.req_id,
            200,
            new IpcHeaders()
              .init("Access-Control-Allow-Origin", "*")
              .init("Access-Control-Allow-Headers", "*")
              .init("Access-Control-Allow-Methods", "*"),
            "",
            ipc
          )
        );
      }
      const url = request.parsed_url;
      // serviceWorker
      if (url.pathname.startsWith("/dns.sys.dweb")) {
        const result = await serviceWorkerFactory(url);
        const ipcResponse = IpcResponse.fromText(
          request.req_id,
          200,
          undefined,
          result,
          ipc
        );
        cros(ipcResponse.headers);
        // 返回数据到前端
        return ipc.postMessage(ipcResponse);
      }
      onApiRequest(apiServer.startResult.urlInfo, request, ipc);
    });

    // 转发serviceWorker 请求
    const serviceWorkerFactory = async (url: URL) => {
      const pathname = url.pathname;
      // 关闭的流程需要调整
      // 向dns发送关闭当前 模块的消息
      // woker.js -> dns -> JsMicroModule -> woker.js -> 其他的 NativeMicroModule

      if (pathname.endsWith("restart")) {
        // 关闭全部的服务
        await apiServer.close();
        await wwwServer.close();
        await externalServer.close();
        // 关闭所有的DwebView
        await mwebview_destroy();
        // 这里只需要把请求发送过去，因为app已经被关闭，已经无法拿到返回值
        jsProcess.restart();
        return "restart ok";
      }

      // 只关闭 渲染一个渲染进程 不关闭 service
      if (pathname.endsWith("close")) {
        await mwebview_destroy();
        return "window close";
      }
      return "no action for serviceWorker Factory !!!";
    };
  }
}

import { IpcHeaders } from "../../../desktop-dev/src/core/ipc/IpcHeaders.ts";
import { $DwebHttpServerOptions } from "../../../desktop-dev/src/sys/http-server/net/createNetServer.ts";
import { OBSERVE } from "./const.ts";
import {
  $MMID,
  createSignal,
  mapHelper,
  PromiseOut,
  ReadableStreamOut,
  simpleEncoder,
  u8aConcat,
} from "./deps.ts";
import { mwebview_destroy } from "./mwebview-helper.ts";

const ipcObserversMap = new Map<
  $MMID,
  {
    ipc: PromiseOut<$Ipc>;
    obs: Set<{ controller: ReadableStreamDefaultController<Uint8Array> }>;
  }
>();

const INTERNAL_PREFIX = "/internal";
type $OnIpcRequestUrl = (request: $IpcRequest) => void;
export const fetchSignal = createSignal<$OnIpcRequestUrl>();

/**
 * request 事件处理器
 */
export async function onApiRequest(
  serverurlInfo: $ServerUrlInfo,
  request: $IpcRequest,
  httpServerIpc: $Ipc
) {
  let ipcResponse: undefined | $IpcResponse;
  const url = request.parsed_url;
  try {
    // 是否是内部请求
    if (url.pathname.startsWith(INTERNAL_PREFIX)) {
      ipcResponse = internalFactory(
        url,
        request.req_id,
        httpServerIpc,
        serverurlInfo
      );
    } else {
      // 转发file请求到目标NMM
      const path = `file:/${url.pathname}${url.search}`;
      const ipcProxyRequest = new IpcRequest(
        jsProcess.fetchIpc.allocReqId(),
        path,
        request.method,
        request.headers,
        request.body,
        jsProcess.fetchIpc
      );
      // 必须要直接向目标对发连接 通过这个 IPC 发送请求
      const targetIpc = await jsProcess.connect(
        ipcProxyRequest.parsed_url.host as $MMID
      );
      targetIpc.postMessage(ipcProxyRequest);
      const ipcProxyResponse = await targetIpc.registerReqId(
        ipcProxyRequest.req_id
      ).promise;
      ipcResponse = new IpcResponse(
        request.req_id,
        ipcProxyResponse.statusCode,
        ipcProxyResponse.headers,
        ipcProxyResponse.body,
        httpServerIpc
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

/**处理内部的绑定流事件 */
const internalFactory = (
  url: URL,
  req_id: number,
  httpServerIpc: $Ipc,
  serverurlInfo: $ServerUrlInfo
) => {
  const pathname = url.pathname.slice(INTERNAL_PREFIX.length);
  // 转发public url
  if (pathname === "/public-url") {
    return IpcResponse.fromText(
      req_id,
      200,
      undefined,
      serverurlInfo.buildPublicUrl(() => {}).href,
      httpServerIpc
    );
  }
  // 监听属性
  if (pathname === "/observe") {
    const mmid = url.searchParams.get("mmid") as $MMID;
    if (mmid === null) {
      throw new Error("observe require mmid");
    }
    const streamPo = observeFactory(mmid);
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
    // serviceWorker fetch
    const streamPo = serviceWorkerFetch();
    return IpcResponse.fromStream(
      req_id,
      200,
      undefined,
      streamPo.stream,
      httpServerIpc
    );
  }
};

/**这里会处理api的消息返回到前端serviceWorker 构建onFetchEvent 并触发fetch事件 */
const serviceWorkerFetch = () => {
  const streamPo = new ReadableStreamOut<Uint8Array>();
  const ob = { controller: streamPo.controller };
  fetchSignal.listen((ipcRequest) => {
    const jsonlineEnd = simpleEncoder("\n", "utf8");
    const json = ipcRequest.toJSON();
    const uint8 = simpleEncoder(JSON.stringify(json), "utf8");
    ob.controller.enqueue(u8aConcat([uint8, jsonlineEnd]));
  });
  return streamPo;
};

/**监听属性的变化 */
const observeFactory = (mmid: $MMID) => {
  const streamPo = new ReadableStreamOut<Uint8Array>();
  const observers = mapHelper.getOrPut(ipcObserversMap, mmid, (mmid) => {
    const result = { ipc: new PromiseOut<$Ipc>(), obs: new Set() };
    result.ipc.resolve(jsProcess.connect(mmid));
    result.ipc.promise.then((ipc) => {
      ipc.onEvent((event) => {
        if (event.name !== OBSERVE.State) {
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
  return streamPo;
};
