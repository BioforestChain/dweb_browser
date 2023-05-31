import type { IpcResponse } from "../../core/ipc/IpcResponse.ts";
import { PromiseOut } from "../../helper/PromiseOut.ts";
import { closeApp, closeFront, webViewMap } from "../tool/app.handle.ts";
import { cros, nativeActivate, nativeOpen } from "../tool/tool.native.ts";
import { $Ipc, fetchSignal, onApiRequest } from "../tool/tool.request.ts";

const main = async () => {
  const { IpcEvent } = ipc;
  // 启动主页面的地址
  const mainUrl = new PromiseOut<string>();
  const EXTERNAL_PREFIX = "/external/";

  /**尝试打开view */
  const tryOpenView = async () => {
    if (webViewMap.size === 0) {
      // open
      const url = await mainUrl.promise;
      const view_id = await nativeOpen(url);
      webViewMap.set(view_id, {
        isActivated: true,
        webviewId: view_id,
      });
      return view_id;
    }
    // 当前的策略是有多少个webview激活多少个
    await Promise.all(
      [...webViewMap.values()].map((item) => {
        // activate
        return nativeActivate(item.webviewId);
      })
    );
  };

  const { IpcResponse } = ipc;
  /**给前端的文件服务 */
  const wwwServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "www",
    port: 443,
  });
  console.log("🇨🇳 wwwServer=>",wwwServer.startResult.urlInfo.internal_origin)
  /**给前端的api服务 */
  const apiServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "api",
    port: 443,
  });
  /**给前端的api服务 */
  const externalServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "external",
    port: 443,
  });

  // 自己api处理 Fetch
  const apiReadableStreamIpc = await apiServer.listen();
  // 文件服务处理
  const wwwReadableStreamIpc = await wwwServer.listen();
  // 别滴app发送到请求走这里发送到前端的DwebServiceWorker fetch
  const externalReadableStreamIpc = await externalServer.listen();

  apiReadableStreamIpc.onRequest(async (request, ipc) => {
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

  wwwReadableStreamIpc.onRequest(async (request, ipc) => {
    let pathname = request.parsed_url.pathname;
    if (pathname === "/") {
      pathname = "/index.html";
    }
    const remoteIpcResponse = await jsProcess.nativeRequest(
      `file:///jmm/cot-demo${pathname}?mode=stream`
    );
    ipc.postMessage(
      new IpcResponse(
        request.req_id,
        remoteIpcResponse.statusCode,
        cros(remoteIpcResponse.headers),
        remoteIpcResponse.body,
        ipc
      )
    );
  });

  const externalMap = new Map<number, PromiseOut<IpcResponse>>();
  // 提供APP之间通信的方法
  externalReadableStreamIpc.onRequest(async (request, ipc) => {
    const url = request.parsed_url;
    const xHost = decodeURIComponent(url.searchParams.get("X-Dweb-Host") ?? "");

    // 处理serviceworker respondWith过来的请求,回复给别的app
    if (url.pathname.startsWith(EXTERNAL_PREFIX)) {
      const pathname = url.pathname.slice(EXTERNAL_PREFIX.length);
      const externalReqId = parseInt(pathname);
      // 验证传递的reqId
      if (typeof externalReqId !== "number" || isNaN(externalReqId)) {
        return ipc.postMessage(
          IpcResponse.fromText(
            request.req_id,
            400,
            request.headers,
            "reqId is NAN",
            ipc
          )
        );
      }
      const responsePOo = externalMap.get(externalReqId);
      // 验证是否有外部请求
      if (!responsePOo) {
        return ipc.postMessage(
          IpcResponse.fromText(
            request.req_id,
            500,
            request.headers,
            `not found external requst,req_id ${externalReqId}`,
            ipc
          )
        );
      }
      // 转发给外部的app
      responsePOo.resolve(
        new IpcResponse(externalReqId, 200, request.headers, request.body, ipc)
      );
      externalMap.delete(externalReqId);
      const icpResponse = IpcResponse.fromText(
        request.req_id,
        200,
        request.headers,
        "ok",
        ipc
      );
      cros(icpResponse.headers);
      // 告知自己的 respondWith 已经发送成功了
      return ipc.postMessage(icpResponse);
    }

    // 别的app发送消息，触发一下前端注册的fetch
    if (xHost === externalServer.startResult.urlInfo.host) {
      fetchSignal.emit(request);
      const awaitResponse = new PromiseOut<IpcResponse>();
      externalMap.set(request.req_id, awaitResponse);
      const ipcResponse = await awaitResponse.promise;
      cros(ipcResponse.headers);
      // 返回数据到发送者那边
      ipc.postMessage(ipcResponse);
    }
  });

  // 转发serviceWorker 请求
  const serviceWorkerFactory = async (url: URL) => {
    const pathname = url.pathname;
    // 关闭前端
    if (pathname.endsWith("close")) {
      return await closeFront();
    }
    // 重启app，伴随着前后端重启
    if (pathname.endsWith("restart")) {
      // 关闭所有的DwebView
      await closeFront();
      closeApp(
        [apiServer, wwwServer, externalServer]
      );
      // 这里只需要把请求发送过去，因为app已经被关闭，已经无法拿到返回值
      jsProcess.restart();

      return "restart ok";
    }
    return "no action for serviceWorker Factory !!!";
  };
  /// 如果有人来激活，那我就唤醒我的界面
  jsProcess.onActivity(async (_ipcEvent, ipc) => {
    await tryOpenView();
    ipc.postMessage(IpcEvent.fromText("ready", "activity"));
    if (hasActivityEventIpcs.has(ipc) === false) {
      hasActivityEventIpcs.add(ipc);
    }
  });
  const hasActivityEventIpcs = new Set<$Ipc>();
  jsProcess.onClose((_event, ipc) => {
    // 接收JMM更新程序的关闭消息（安装完新的app需要重启应用）
    if (ipc.remote.mmid === "jmm.sys.dweb") {
      return closeApp(
        [apiServer, wwwServer, externalServer]
      );
    }
  });
  const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
    url.pathname = "/index.html";
  });
  interUrl.searchParams.set(
    "X-Plaoc-Internal-Url",
    apiServer.startResult.urlInfo.buildInternalUrl().href
  );
  interUrl.searchParams.set(
    "X-Plaoc-Public-Url",
    apiServer.startResult.urlInfo.buildPublicUrl().href
  );
  mainUrl.resolve(interUrl.href);
  /**
   * 立刻自启动
   */
  tryOpenView();
};

main();
